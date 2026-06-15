package com.nguyenquyen.chatservice.message;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.nguyenquyen.chatservice.chat.Chat;
import com.nguyenquyen.chatservice.chat.ChatService;
import com.nguyenquyen.chatservice.kafka.ChatEvent;
import com.nguyenquyen.chatservice.kafka.ChatEventPublisher;
import com.nguyenquyen.chatservice.kafka.ChatEventType;
import com.nguyenquyen.chatservice.presence.PresenceService;
import com.nguyenquyen.chatservice.util.SecurityUtils;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class MessageServiceImpl implements MessageService {

    private final MessageRepository messageRepository;
    private final MessageMapper messageMapper;
    private final ChatService chatService;
    private final PresenceService presenceService;
    private final ChatEventPublisher eventPublisher;

    @Override
    public MessageResponse sendMessage(UUID chatId, String content, MessageType type, String imageId) {
        String currentUserId = SecurityUtils.requireCurrentUserId();
        return sendMessageInternal(chatId, currentUserId, content, type, imageId);
    }

    @Override
    public MessageResponse sendMessage(UUID chatId, String senderId, String content, MessageType type, String imageId) {
        return sendMessageInternal(chatId, senderId, content, type, imageId);
    }

    private MessageResponse sendMessageInternal(UUID chatId, String senderId, String content, MessageType type, String imageId) {
        Chat chat = chatService.getChatEntityById(chatId);

        validateParticipant(chat, senderId);

        Message message = messageMapper.toEntity(
                chat, senderId, content, type, imageId
        );

        Message saved = messageRepository.save(message);
        log.info("Message saved: id={}, chatId={}, from={}, type={}",
                saved.getId(), chatId, senderId, type);

        MessageResponse response = messageMapper.toResponse(saved);

        // Notify all other members
        List<String> recipients = chat.getMemberIds().stream()
                .filter(id -> !id.equals(senderId))
                .toList();

        // ALWAYS publish one event for Search Service indexing
        eventPublisher.publish(ChatEvent.builder()
                .type(ChatEventType.NEW_MESSAGE.name())
                .chatId(chatId)
                .messageId(saved.getId())
                .actorId(senderId)
                .recipientId(null) // Null recipient signals this is a broadcast/system event
                .previewText(getPreviewContent(content, type))
                .previewImageId(type == MessageType.IMAGE ? imageId : null)
                .messageType(type.name())
                .build());
        log.debug("Published base NEW_MESSAGE event to Kafka for Search Service");

        for (String recipientId : recipients) {
            boolean recipientInChat = presenceService.isUserInChat(recipientId, chatId);

            if (!recipientInChat) {
                eventPublisher.publish(ChatEvent.builder()
                        .type(ChatEventType.NEW_MESSAGE.name())
                        .chatId(chatId)
                        .messageId(saved.getId())
                        .actorId(senderId)
                        .recipientId(recipientId)
                        .previewText(getPreviewContent(content, type))
                        .previewImageId(type == MessageType.IMAGE ? imageId : null)
                        .messageType(type.name())
                        .build());
                log.debug("Published NEW_MESSAGE event to Kafka for recipient {}", recipientId);
            }
        }

        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<MessageResponse> getChatMessages(UUID chatId, Pageable pageable) {
        String currentUserId = SecurityUtils.requireCurrentUserId();
        Chat chat = chatService.getChatEntityById(chatId);
        validateParticipant(chat, currentUserId);

        Page<MessageResponse> result = messageRepository.findByChatId(chatId, pageable)
                .map(messageMapper::toResponse);

        log.debug("Retrieved {} messages for chat {} (page {}, size {})",
                result.getNumberOfElements(), chatId, pageable.getPageNumber(), pageable.getPageSize());

        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public List<MessageResponse> getAllChatMessages(UUID chatId) {
        String currentUserId = SecurityUtils.requireCurrentUserId();
        Chat chat = chatService.getChatEntityById(chatId);
        validateParticipant(chat, currentUserId);

        List<Message> messages = messageRepository.findAllByChatIdOrdered(chatId);
        log.debug("Retrieved all {} messages for chat {}", messages.size(), chatId);

        return messageMapper.toResponseList(messages);
    }

    @Override
    public int markAsRead(UUID chatId) {
        String currentUserId = SecurityUtils.requireCurrentUserId();
        return markAsReadInternal(chatId, currentUserId);
    }

    @Override
    public int markAsRead(UUID chatId, String userId) {
        return markAsReadInternal(chatId, userId);
    }

    private int markAsReadInternal(UUID chatId, String userId) {
        Chat chat = chatService.getChatEntityById(chatId);
        validateParticipant(chat, userId);

        int updated = messageRepository.markMessagesAsRead(
                chatId, userId, MessageState.SENT, MessageState.READ
        );

        if (updated > 0) {
            // Notify other members that this user has read messages
            List<String> recipients = chat.getMemberIds().stream()
                    .filter(id -> !id.equals(userId))
                    .toList();

            for (String recipientId : recipients) {
                eventPublisher.publish(ChatEvent.builder()
                        .type(ChatEventType.MESSAGES_READ.name())
                        .chatId(chatId)
                        .actorId(userId)
                        .recipientId(recipientId)
                        .build());
            }

            log.info("Marked {} messages as read in chat {} by user {}", updated, chatId, userId);
        }

        return updated;
    }

    @Override
    @Transactional(readOnly = true)
    public int getUnreadCount(UUID chatId) {
        String currentUserId = SecurityUtils.requireCurrentUserId();
        return getUnreadCountInternal(chatId, currentUserId);
    }

    @Override
    @Transactional(readOnly = true)
    public int getUnreadCount(UUID chatId, String userId) {
        return getUnreadCountInternal(chatId, userId);
    }

    private int getUnreadCountInternal(UUID chatId, String userId) {
        Chat chat = chatService.getChatEntityById(chatId);
        validateParticipant(chat, userId);

        return messageRepository.countUnreadMessages(chatId, userId, MessageState.SENT);
    }

    @Override
    @Transactional(readOnly = true)
    public int getMessagePage(UUID chatId, UUID messageId, int size) {
        String currentUserId = SecurityUtils.requireCurrentUserId();
        Chat chat = chatService.getChatEntityById(chatId);
        validateParticipant(chat, currentUserId);

        long newerCount = messageRepository.countNewerMessages(chatId, messageId);
        return (int) (newerCount / size);
    }

    private void validateParticipant(Chat chat, String userId) {
        if (chat.isNotMember(userId)) {
            throw new AccessDeniedException("You are not a participant of this chat");
        }
    }

    private String getPreviewContent(String content, MessageType type) {
        if (type != MessageType.TEXT && type != MessageType.SYSTEM) {
            return "📎 " + type.name().toLowerCase();
        }
        if (content == null || content.isEmpty()) {
            return "";
        }
        return content.length() > 100 ? content.substring(0, 100) + "..." : content;
    }
}
