package com.nguyenquyen.chatservice.chat;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import com.nguyenquyen.chatservice.exception.BadRequestException;
import com.nguyenquyen.chatservice.exception.ResourceNotFoundException;
import com.nguyenquyen.chatservice.util.SecurityUtils;
import com.nguyenquyen.chatservice.websocket.WsOutgoingMessage;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ChatServiceImpl implements ChatService {

    private final ChatRepository chatRepository;
    private final ChatMemberRepository chatMemberRepository;
    private final ChatMapper chatMapper;
    private final com.nguyenquyen.chatservice.kafka.ChatEventPublisher eventPublisher;
    private final SimpMessagingTemplate messagingTemplate;

    @Override
    public ChatResponse getOrCreatePrivateChat(String targetUserId) {
        String currentUserId = SecurityUtils.requireCurrentUserId();

        if (currentUserId.equals(targetUserId)) {
            throw new BadRequestException("Cannot create chat with yourself");
        }

        Chat chat = chatRepository.findPrivateChatBetween(currentUserId, targetUserId)
                .orElseGet(() -> createNewPrivateChat(currentUserId, targetUserId));

        log.info("Private chat retrieved/created: {} between {} and {}",
                chat.getId(), currentUserId, targetUserId);

        return chatMapper.toResponse(chat, currentUserId);
    }

    @Override
    public ChatResponse createGroupChat(CreateGroupRequest request) {
        String currentUserId = SecurityUtils.requireCurrentUserId();

        if (request.memberIds() == null || request.memberIds().isEmpty()) {
            throw new BadRequestException("Group must have at least one member");
        }

        Chat chat = Chat.builder()
                .type(ChatType.GROUP)
                .name(request.name())
                .imageId(request.imageId())
                .ownerId(currentUserId)
                .build();

        Chat savedChat = chatRepository.save(chat);

        // Add owner
        ChatMember owner = ChatMember.builder()
                .chat(savedChat)
                .userId(currentUserId)
                .role(ChatMemberRole.OWNER)
                .joinedAt(Instant.now())
                .build();
        savedChat.addMember(owner);

        // Add other members
        for (String memberId : request.memberIds()) {
            if (!memberId.equals(currentUserId)) {
                ChatMember member = ChatMember.builder()
                        .chat(savedChat)
                        .userId(memberId)
                        .role(ChatMemberRole.MEMBER)
                        .joinedAt(Instant.now())
                        .build();
                savedChat.addMember(member);
            }
        }

        savedChat = chatRepository.save(savedChat);

        for (String memberId : request.memberIds()) {
            if (!memberId.equals(currentUserId)) {
                eventPublisher.publish(com.nguyenquyen.chatservice.kafka.ChatEvent.builder()
                        .type(com.nguyenquyen.chatservice.kafka.ChatEventType.GROUP_CREATED.name())
                        .chatId(savedChat.getId())
                        .actorId(currentUserId)
                        .recipientId(memberId)
                        .build());
            }
        }

        log.info("Group chat created: {} by {}", savedChat.getId(), currentUserId);

        return chatMapper.toResponse(savedChat, currentUserId);
    }

    @Override
    public ChatResponse updateGroupChat(UUID chatId, UpdateGroupRequest request) {
        String currentUserId = SecurityUtils.requireCurrentUserId();
        Chat chat = getChatEntityById(chatId);

        if (!chat.isGroup()) {
            throw new BadRequestException("Cannot update a private chat");
        }

        ChatMember currentMember = getChatMember(chatId, currentUserId);
        if (currentMember.getRole() == ChatMemberRole.MEMBER) {
            throw new AccessDeniedException("Only ADMIN or OWNER can update group info");
        }

        if (request.name() != null) {
            chat.setName(request.name());
        }
        if (request.imageId() != null) {
            chat.setImageId(request.imageId());
        }

        chat = chatRepository.save(chat);
        log.info("Group chat updated: {} by {}", chatId, currentUserId);
        
        ChatResponse response = chatMapper.toResponse(chat, currentUserId);
        
        // Broadcast GROUP_UPDATED via WebSocket
        WsOutgoingMessage wsMessage = WsOutgoingMessage.groupUpdated(chatId, response);
        chat.getMemberIds().forEach(memberId -> {
            messagingTemplate.convertAndSend("/queue/chat/" + memberId, wsMessage);
        });

        // Publish to Kafka (for offline notifications)
        chat.getMemberIds().forEach(memberId -> {
            if (!memberId.equals(currentUserId)) {
                eventPublisher.publish(com.nguyenquyen.chatservice.kafka.ChatEvent.builder()
                        .type(com.nguyenquyen.chatservice.kafka.ChatEventType.GROUP_UPDATED.name())
                        .chatId(chatId)
                        .actorId(currentUserId)
                        .recipientId(memberId)
                        .build());
            }
        });

        return response;
    }

    @Override
    public ChatResponse addMembers(UUID chatId, AddMembersRequest request) {
        String currentUserId = SecurityUtils.requireCurrentUserId();
        Chat chat = getChatEntityById(chatId);

        if (!chat.isGroup()) {
            throw new BadRequestException("Cannot add members to a private chat");
        }

        ChatMember currentMember = getChatMember(chatId, currentUserId);
        if (currentMember.getRole() == ChatMemberRole.MEMBER) {
            throw new AccessDeniedException("Only ADMIN or OWNER can add members");
        }

        for (String memberId : request.memberIds()) {
            if (chat.isNotMember(memberId)) {
                ChatMember newMember = ChatMember.builder()
                        .chat(chat)
                        .userId(memberId)
                        .role(ChatMemberRole.MEMBER)
                        .joinedAt(Instant.now())
                        .build();
                chat.addMember(newMember);
            }
        }

        chat = chatRepository.save(chat);

        for (String memberId : request.memberIds()) {
            eventPublisher.publish(com.nguyenquyen.chatservice.kafka.ChatEvent.builder()
                    .type(com.nguyenquyen.chatservice.kafka.ChatEventType.MEMBER_ADDED.name())
                    .chatId(chatId)
                    .actorId(currentUserId)
                    .recipientId(memberId)
                    .build());
        }

        log.info("Members added to group chat {}: {}", chatId, request.memberIds());

        ChatResponse response = chatMapper.toResponse(chat, currentUserId);

        // Broadcast MEMBER_CHANGE via WebSocket
        WsOutgoingMessage wsMessage = WsOutgoingMessage.memberChange(chatId, currentUserId, request.memberIds());
        chat.getMemberIds().forEach(memberId -> {
            messagingTemplate.convertAndSend("/queue/chat/" + memberId, wsMessage);
        });

        return response;
    }

    @Override
    public void removeMember(UUID chatId, String memberId) {
        String currentUserId = SecurityUtils.requireCurrentUserId();
        Chat chat = getChatEntityById(chatId);

        if (!chat.isGroup()) {
            throw new BadRequestException("Cannot remove members from a private chat");
        }

        ChatMember currentMember = getChatMember(chatId, currentUserId);
        if (currentMember.getRole() == ChatMemberRole.MEMBER) {
            throw new AccessDeniedException("Only ADMIN or OWNER can remove members");
        }

        ChatMember memberToRemove = getChatMember(chatId, memberId);
        if (memberToRemove.getRole() == ChatMemberRole.OWNER && !currentUserId.equals(memberId)) {
            throw new AccessDeniedException("Cannot remove the OWNER of the group");
        }

        chat.getMembers().remove(memberToRemove);
        chatRepository.save(chat);

        eventPublisher.publish(com.nguyenquyen.chatservice.kafka.ChatEvent.builder()
                .type(com.nguyenquyen.chatservice.kafka.ChatEventType.MEMBER_REMOVED.name())
                .chatId(chatId)
                .actorId(currentUserId)
                .recipientId(memberId)
                .build());

        // Broadcast MEMBER_CHANGE via WebSocket
        WsOutgoingMessage wsMessage = WsOutgoingMessage.memberChange(chatId, currentUserId, List.of(memberId));
        chat.getMemberIds().forEach(id -> {
            messagingTemplate.convertAndSend("/queue/chat/" + id, wsMessage);
        });
        // Also send to the removed member so their client knows they were removed
        messagingTemplate.convertAndSend("/queue/chat/" + memberId, wsMessage);

        log.info("Member {} removed from group chat {} by {}", memberId, chatId, currentUserId);
    }

    @Override
    public ChatResponse changeMemberRole(UUID chatId, String targetUserId, ChangeRoleRequest request) {
        String currentUserId = SecurityUtils.requireCurrentUserId();
        Chat chat = getChatEntityById(chatId);

        if (!chat.isGroup()) {
            throw new BadRequestException("Cannot change roles in a private chat");
        }

        ChatMember currentMember = getChatMember(chatId, currentUserId);
        if (currentMember.getRole() != ChatMemberRole.OWNER) {
            throw new AccessDeniedException("Only the OWNER can change member roles");
        }

        if (currentUserId.equals(targetUserId)) {
            throw new BadRequestException("Cannot change your own role");
        }

        ChatMember targetMember = getChatMember(chatId, targetUserId);
        targetMember.setRole(request.role());
        
        chat = chatRepository.save(chat);
        log.info("Changed role of member {} to {} in group chat {} by {}", targetUserId, request.role(), chatId, currentUserId);
        
        ChatResponse response = chatMapper.toResponse(chat, currentUserId);

        // Broadcast MEMBER_CHANGE via WebSocket
        WsOutgoingMessage wsMessage = WsOutgoingMessage.memberChange(chatId, currentUserId, List.of(targetUserId));
        chat.getMemberIds().forEach(id -> {
            messagingTemplate.convertAndSend("/queue/chat/" + id, wsMessage);
        });

        // Publish to Kafka
        eventPublisher.publish(com.nguyenquyen.chatservice.kafka.ChatEvent.builder()
                .type(com.nguyenquyen.chatservice.kafka.ChatEventType.GROUP_UPDATED.name())
                .chatId(chatId)
                .actorId(currentUserId)
                .recipientId(targetUserId)
                .build());

        return response;
    }

    @Override
    public void leaveChat(UUID chatId) {
        String currentUserId = SecurityUtils.requireCurrentUserId();
        Chat chat = getChatEntityById(chatId);

        if (!chat.isGroup()) {
            throw new BadRequestException("Cannot leave a private chat");
        }

        ChatMember currentMember = getChatMember(chatId, currentUserId);
        
        if (currentMember.getRole() == ChatMemberRole.OWNER) {
            throw new BadRequestException("OWNER cannot leave the group. Transfer ownership or delete the group.");
        }

        chat.getMembers().remove(currentMember);
        chatRepository.save(chat);

        // Broadcast MEMBER_CHANGE via WebSocket
        WsOutgoingMessage wsMessage = WsOutgoingMessage.memberChange(chatId, currentUserId, List.of(currentUserId));
        chat.getMemberIds().forEach(id -> {
            messagingTemplate.convertAndSend("/queue/chat/" + id, wsMessage);
        });
        messagingTemplate.convertAndSend("/queue/chat/" + currentUserId, wsMessage);

        log.info("User {} left group chat {}", currentUserId, chatId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ChatResponse> getMyChats() {
        String currentUserId = SecurityUtils.requireCurrentUserId();
        List<Chat> chats = chatRepository.findAllByUserIdOrderedByLastMessage(currentUserId);
        return chatMapper.toResponseList(chats, currentUserId);
    }

    @Override
    @Transactional(readOnly = true)
    public ChatResponse getChatById(UUID chatId) {
        String currentUserId = SecurityUtils.requireCurrentUserId();
        Chat chat = getChatEntityById(chatId);
        
        if (chat.isNotMember(currentUserId)) {
             throw new AccessDeniedException("You are not a participant of this chat");
        }
        
        return chatMapper.toResponse(chat, currentUserId);
    }

    @Override
    public void deleteChat(UUID chatId) {
        String currentUserId = SecurityUtils.requireCurrentUserId();
        Chat chat = getChatEntityById(chatId);

        if (chat.isPrivate()) {
            if (chat.isNotMember(currentUserId)) {
                throw new AccessDeniedException("You are not a participant of this chat");
            }
        } else {
            ChatMember currentMember = getChatMember(chatId, currentUserId);
            if (currentMember.getRole() != ChatMemberRole.OWNER) {
                throw new AccessDeniedException("Only the OWNER can delete the group chat");
            }
        }

        chatRepository.delete(chat);
        log.info("Chat {} deleted by user {}", chatId, currentUserId);
    }

    @Override
    @Transactional(readOnly = true)
    public Chat getChatEntityById(UUID chatId) {
        Chat chat = chatRepository.findById(chatId)
                .orElseThrow(() -> new ResourceNotFoundException("Chat not found: " + chatId));
        // Force initialization of members collection to avoid LazyInitializationException in controllers
        chat.getMembers().size();
        return chat;
    }

    private Chat createNewPrivateChat(String senderId, String recipientId) {
        Chat chat = Chat.builder()
                .type(ChatType.PRIVATE)
                .ownerId(senderId) // Setting owner to creator
                .build();

        Chat savedChat = chatRepository.save(chat);

        ChatMember member1 = ChatMember.builder()
                .chat(savedChat)
                .userId(senderId)
                .role(ChatMemberRole.MEMBER)
                .joinedAt(Instant.now())
                .build();

        ChatMember member2 = ChatMember.builder()
                .chat(savedChat)
                .userId(recipientId)
                .role(ChatMemberRole.MEMBER)
                .joinedAt(Instant.now())
                .build();

        savedChat.addMember(member1);
        savedChat.addMember(member2);

        savedChat = chatRepository.save(savedChat);
        log.info("New private chat created: {} between {} and {}", savedChat.getId(), senderId, recipientId);
        return savedChat;
    }

    private ChatMember getChatMember(UUID chatId, String userId) {
        return chatMemberRepository.findByChatIdAndUserId(chatId, userId)
                .orElseThrow(() -> new AccessDeniedException("You are not a member of this chat"));
    }
}
