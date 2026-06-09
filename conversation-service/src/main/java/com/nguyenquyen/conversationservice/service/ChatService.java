package com.nguyenquyen.conversationservice.service;


import com.nguyenquyen.conversationservice.dto.ChatResponse;
import com.nguyenquyen.conversationservice.dto.CreatePrivateChatRequest;
import com.nguyenquyen.conversationservice.dto.ParticipantResponse;
import com.nguyenquyen.conversationservice.entity.Chat;
import com.nguyenquyen.conversationservice.entity.ChatParticipant;
import com.nguyenquyen.conversationservice.entity.ChatType;
import com.nguyenquyen.conversationservice.event.ChatCreatedEvent;
import com.nguyenquyen.conversationservice.exception.ChatAccessDeniedException;
import com.nguyenquyen.conversationservice.exception.ChatNotFoundException;
import com.nguyenquyen.conversationservice.repository.ChatRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;


@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ChatService {

    private static final String CHAT_EVENTS_TOPIC = "chat-events";

    private final ChatRepository chatRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    // ─── Create or Get Private Chat ────────────────────────────────────────────

    /**
     * Creates a new private chat between the authenticated user and a target user.
     * If a private chat already exists between them, returns the existing one (idempotent).
     *
     * @param requesterId authenticated user's UUID (from X-User-Id header)
     * @param request     contains the targetUserId to chat with
     * @return existing or newly created chat
     * @throws IllegalArgumentException if user tries to chat with themselves
     */
    @Transactional
    public ChatResponse createOrGetPrivateChat(UUID requesterId, CreatePrivateChatRequest request) {
        UUID targetUserId = request.getTargetUserId();

        if (requesterId.equals(targetUserId)) {
            throw new IllegalArgumentException("You cannot start a chat with yourself");
        }

        // Idempotent: return existing private chat if one already exists
        return chatRepository.findPrivateChatBetweenUsers(requesterId, targetUserId, ChatType.PRIVATE)
                .map(existingChat -> {
                    log.debug("Returning existing private chat {} between {} and {}",
                            existingChat.getId(), requesterId, targetUserId);
                    return toResponse(existingChat);
                })
                .orElseGet(() -> {
                    // Create new private chat — add participants to collection BEFORE saving
                    // so CascadeType.ALL persists them and they remain in-memory immediately
                    Chat chat = Chat.builder()
                            .type(ChatType.PRIVATE)
                            .build();

                    ChatParticipant p1 = ChatParticipant.builder()
                            .chat(chat)
                            .userId(requesterId)
                            .build();
                    ChatParticipant p2 = ChatParticipant.builder()
                            .chat(chat)
                            .userId(targetUserId)
                            .build();

                    chat.getParticipants().add(p1);
                    chat.getParticipants().add(p2);

                    Chat savedChat = chatRepository.save(chat);  // cascades → saves participants too

                    // Publish Kafka event
                    publishChatCreatedEvent(savedChat);

                    log.info("Private chat created: chatId={} between {} and {}",
                            savedChat.getId(), requesterId, targetUserId);
                    return toResponse(savedChat);
                });
    }

    // ─── Create Group Chat ─────────────────────────────────────────────────────

    /**
     * Creates a GROUP type chat with all provided member IDs as participants.
     * Called via REST endpoint (service-to-service).
     *
     * @param memberIds all group member UUIDs (must include the creator)
     * @return the newly created GROUP chat
     */
    @Transactional
    public ChatResponse createGroupChat(List<UUID> memberIds) {
        Chat chat = Chat.builder()
                .type(ChatType.GROUP)
                .build();

        for (UUID memberId : memberIds) {
            ChatParticipant p = ChatParticipant.builder()
                    .chat(chat)
                    .userId(memberId)
                    .build();
            chat.getParticipants().add(p);
        }

        Chat saved = chatRepository.save(chat);
        publishChatCreatedEvent(saved);

        log.info("Group chat created: chatId={}, members={}", saved.getId(), memberIds.size());
        return toResponse(saved);
    }

    /**
     * Creates a GROUP chat linked to a specific groupId.
     * Idempotent: if a chat already exists for this groupId, returns it.
     * Called by {@code GroupEventConsumer} when a {@code GroupCreatedEvent} is received.
     *
     * @param groupId   the group UUID from group-service
     * @param memberIds all member UUIDs to add as participants
     * @return the created (or existing) chat response
     */
    @Transactional
    public ChatResponse createGroupChat(UUID groupId, List<UUID> memberIds) {
        // Idempotency check
        Optional<Chat> existing = chatRepository.findByGroupId(groupId);
        if (existing.isPresent()) {
            log.warn("Group chat already exists for groupId={} — skipping duplicate", groupId);
            return toResponse(existing.get());
        }

        Chat chat = Chat.builder()
                .type(ChatType.GROUP)
                .groupId(groupId)
                .build();

        for (UUID memberId : memberIds) {
            ChatParticipant p = ChatParticipant.builder()
                    .chat(chat)
                    .userId(memberId)
                    .build();
            chat.getParticipants().add(p);
        }

        Chat saved = chatRepository.save(chat);
        publishChatCreatedEvent(saved);

        log.info("Group chat created: chatId={}, groupId={}, members={}", saved.getId(), groupId, memberIds.size());
        return toResponse(saved);
    }

    // ─── Group Chat Member Sync ─────────────────────────────────────────────────

    /**
     * Adds a participant to the GROUP chat associated with the given groupId.
     * Idempotent: skips if already a participant.
     *
     * @param groupId the group UUID
     * @param userId  the user to add
     */
    @Transactional
    public void addParticipantToGroupChat(UUID groupId, UUID userId) {
        Chat chat = chatRepository.findByGroupId(groupId)
                .orElseThrow(() -> new ChatNotFoundException("No chat found for groupId: " + groupId));

        boolean alreadyParticipant = chat.getParticipants().stream()
                .anyMatch(p -> p.getUserId().equals(userId));
        if (alreadyParticipant) {
            log.warn("User {} is already a participant in chat for groupId={} — skipping", userId, groupId);
            return;
        }

        ChatParticipant participant = ChatParticipant.builder()
                .chat(chat)
                .userId(userId)
                .build();
        chat.getParticipants().add(participant);
        chatRepository.save(chat);
        log.info("Added participant {} to group chat for groupId={}", userId, groupId);
    }

    /**
     * Removes a participant from the GROUP chat associated with the given groupId.
     * Idempotent: skips if not a participant or if chat doesn't exist.
     *
     * @param groupId the group UUID
     * @param userId  the user to remove
     */
    @Transactional
    public void removeParticipantFromGroupChat(UUID groupId, UUID userId) {
        Optional<Chat> chatOpt = chatRepository.findByGroupId(groupId);
        if (chatOpt.isEmpty()) {
            log.warn("No chat found for groupId={} — skipping remove", groupId);
            return;
        }
        Chat chat = chatOpt.get();

        boolean removed = chat.getParticipants().removeIf(p -> p.getUserId().equals(userId));
        if (removed) {
            chatRepository.save(chat);
            log.info("Removed participant {} from group chat for groupId={}", userId, groupId);
        } else {
            log.warn("User {} is not a participant in chat for groupId={} — skipping", userId, groupId);
        }
    }

    /**
     * Deletes the GROUP chat associated with the given groupId.
     * Called when the group is deleted. Cascades to all participants.
     *
     * @param groupId the group UUID
     */
    @Transactional
    public void deleteGroupChat(UUID groupId) {
        Optional<Chat> chatOpt = chatRepository.findByGroupId(groupId);
        if (chatOpt.isEmpty()) {
            log.warn("No chat found for groupId={} — nothing to delete", groupId);
            return;
        }
        chatRepository.delete(chatOpt.get());
        log.info("Deleted group chat for groupId={}", groupId);
    }

    // ─── Get My Chats ──────────────────────────────────────────────────────────

    /**
     * Returns a paginated list of all chats where the authenticated user is a participant.
     * Results are sorted by most recently updated first.
     *
     * @param userId UUID of the authenticated user
     * @param page   zero-based page index
     * @param size   number of results per page
     * @return paginated list of the user's chats
     */
    public Page<ChatResponse> getMyChats(UUID userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return chatRepository.findAllByParticipantUserId(userId, pageable)
                .map(this::toResponse);
    }

    // ─── Get Chat By ID ────────────────────────────────────────────────────────

    /**
     * Returns details of a specific chat, verifying the caller is a participant.
     *
     * @param chatId UUID of the chat to retrieve
     * @param userId UUID of the authenticated user (must be a participant)
     * @return chat details with participants
     * @throws ChatNotFoundException     if no chat found with the given ID
     * @throws ChatAccessDeniedException if the caller is not a participant
     */
    public ChatResponse getChatById(UUID chatId, UUID userId) {
        Chat chat = findOrThrow(chatId);
        assertParticipant(chat, userId);
        return toResponse(chat);
    }

    // ─── Leave Chat ────────────────────────────────────────────────────────────

    @Transactional
    public void leaveChat(UUID chatId, UUID userId) {
        Chat chat = findOrThrow(chatId);
        assertParticipant(chat, userId);

        chat.getParticipants().removeIf(p -> p.getUserId().equals(userId));
        chatRepository.save(chat);
        log.info("User {} left chat {}", userId, chatId);
    }

    // ─── Private Helpers ──────────────────────────────────────────────────────

    private Chat findOrThrow(UUID chatId) {
        return chatRepository.findById(chatId)
                .orElseThrow(() -> new ChatNotFoundException("Chat not found: " + chatId));
    }

    private void assertParticipant(Chat chat, UUID userId) {
        boolean isParticipant = chat.getParticipants().stream()
                .anyMatch(p -> p.getUserId().equals(userId));
        if (!isParticipant) {
            throw new ChatAccessDeniedException("You are not a participant in this chat");
        }
    }

    private void publishChatCreatedEvent(Chat chat) {
        List<UUID> participantIds = chat.getParticipants().stream()
                .map(ChatParticipant::getUserId)
                .toList();

        ChatCreatedEvent event = ChatCreatedEvent.builder()
                .chatId(chat.getId())
                .type(chat.getType())
                .groupId(chat.getGroupId())
                .participantIds(participantIds)
                .createdAt(chat.getCreatedAt())
                .build();

        kafkaTemplate.send(CHAT_EVENTS_TOPIC, chat.getId().toString(), event);
        log.debug("Published chat-created event: chatId={}", chat.getId());
    }

    private ChatResponse toResponse(Chat chat) {
        List<ParticipantResponse> participants = chat.getParticipants().stream()
                .map(p -> ParticipantResponse.builder()
                        .userId(p.getUserId())
                        .joinedAt(p.getJoinedAt())
                        .build())
                .toList();

        return ChatResponse.builder()
                .id(chat.getId())
                .type(chat.getType())
                .groupId(chat.getGroupId())
                .participants(participants)
                .createdAt(chat.getCreatedAt())
                .updatedAt(chat.getUpdatedAt())
                .build();
    }
}
