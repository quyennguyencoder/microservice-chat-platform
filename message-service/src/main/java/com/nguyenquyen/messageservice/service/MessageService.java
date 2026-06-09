package com.nguyenquyen.messageservice.service;

import com.nguyenquyen.messageservice.entity.MessageStatus;
import com.nguyenquyen.messageservice.entity.MessageType;
import com.nguyenquyen.messageservice.dto.MessageResponse;
import com.nguyenquyen.messageservice.dto.SendMessageRequest;
import com.nguyenquyen.messageservice.dto.UnreadCountResponse;
import com.nguyenquyen.messageservice.entity.Message;
import com.nguyenquyen.messageservice.event.MessageSentEvent;
import com.nguyenquyen.messageservice.exception.MessageAccessDeniedException;
import com.nguyenquyen.messageservice.exception.MessageNotFoundException;
import com.nguyenquyen.messageservice.repository.MessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;


@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class MessageService {

    private static final String MESSAGE_EVENTS_TOPIC = "message-events";

    private final MessageRepository messageRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    // ─── Send Message ──────────────────────────────────────────────────────────

    @Transactional
    public MessageResponse sendMessage(UUID senderId, SendMessageRequest request) {
        MessageType type = request.getType() != null ? request.getType() : MessageType.TEXT;

        Message message = Message.builder()
                .chatId(request.getChatId())
                .senderId(senderId)
                .content(request.getContent())
                .type(type)
                .status(MessageStatus.SENT)
                .build();

        Message saved = messageRepository.save(message);

        // Publish Kafka event — Notification Service (future) will consume this
        publishMessageSentEvent(saved);

        log.info("Message sent: messageId={}, chatId={}, senderId={}",
                saved.getId(), saved.getChatId(), saved.getSenderId());

        return toResponse(saved);
    }

    // ─── Get Messages in a Chat ────────────────────────────────────────────────

    /**
     * Returns paginated messages for a chat, ordered newest-first.
     *
     * @param chatId UUID of the chat to retrieve messages from
     * @param page   zero-based page index
     * @param size   results per page
     * @return paginated messages, newest first
     */
    public Page<MessageResponse> getMessages(UUID chatId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return messageRepository
                .findByChatIdOrderByCreatedAtDesc(chatId, pageable)
                .map(this::toResponse);
    }

    // ─── Mark Chat as Read ─────────────────────────────────────────────────────

    /**
     * Marks all unread messages in a chat as READ for the authenticated user.
     * Only marks messages sent by others — a user's own messages are never auto-marked as read.
     *
     * @param chatId   UUID of the chat
     * @param readerId UUID of the authenticated user marking messages as read
     */
    @Transactional
    public void markChatAsRead(UUID chatId, UUID readerId) {
        messageRepository.markAllAsReadByChatId(chatId, readerId, MessageStatus.READ);
        log.debug("Marked all messages as READ in chat {} for user {}", chatId, readerId);
    }

    // ─── Get Unread Count ──────────────────────────────────────────────────────

    /**
     * Returns the count of unread messages in a chat for the authenticated user.
     *
     * @param chatId UUID of the chat
     * @param userId UUID of the authenticated user
     * @return response with chatId and unread message count
     */
    public UnreadCountResponse getUnreadCount(UUID chatId, UUID userId) {
        long count = messageRepository.countUnreadByChatId(chatId, userId);
        return UnreadCountResponse.builder()
                .chatId(chatId)
                .unreadCount(count)
                .build();
    }

    // ─── Delete Message ────────────────────────────────────────────────────────

    /**
     * Deletes a message. Only the original sender can delete their message.
     *
     * @param messageId UUID of the message to delete
     * @param userId    UUID of the authenticated user (must be the sender)
     * @throws MessageNotFoundException      if no message found with the given ID
     * @throws MessageAccessDeniedException  if the caller is not the sender
     */
    @Transactional
    public void deleteMessage(UUID messageId, UUID userId) {
        Message message = findOrThrow(messageId);

        if (!message.getSenderId().equals(userId)) {
            throw new MessageAccessDeniedException("Only the sender can delete this message");
        }

        messageRepository.delete(message);
        log.info("Message deleted: messageId={} by userId={}", messageId, userId);
    }

    // ─── Private Helpers ──────────────────────────────────────────────────────

    private Message findOrThrow(UUID messageId) {
        return messageRepository.findById(messageId)
                .orElseThrow(() -> new MessageNotFoundException("Message not found: " + messageId));
    }

    private void publishMessageSentEvent(Message message) {
        String preview = message.getContent().length() > 100
                ? message.getContent().substring(0, 100) + "..."
                : message.getContent();

        MessageSentEvent event = MessageSentEvent.builder()
                .messageId(message.getId())
                .chatId(message.getChatId())
                .senderId(message.getSenderId())
                .contentPreview(preview)
                .type(message.getType())
                .createdAt(message.getCreatedAt())
                .build();

        kafkaTemplate.send(MESSAGE_EVENTS_TOPIC, message.getChatId().toString(), event);
        log.debug("Published message-sent event: messageId={}", message.getId());
    }

    private MessageResponse toResponse(Message message) {
        return MessageResponse.builder()
                .id(message.getId())
                .chatId(message.getChatId())
                .senderId(message.getSenderId())
                .content(message.getContent())
                .type(message.getType())
                .status(message.getStatus())
                .createdAt(message.getCreatedAt())
                .updatedAt(message.getUpdatedAt())
                .build();
    }
}
