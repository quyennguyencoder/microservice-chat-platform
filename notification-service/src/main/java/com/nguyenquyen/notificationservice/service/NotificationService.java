package com.nguyenquyen.notificationservice.service;


import com.nguyenquyen.notificationservice.event.ChatCreatedEvent;
import com.nguyenquyen.notificationservice.dto.response.NotificationResponse;
import com.nguyenquyen.notificationservice.dto.response.UnreadCountResponse;
import com.nguyenquyen.notificationservice.entity.ChatParticipantCache;
import com.nguyenquyen.notificationservice.entity.GroupChatMapping;
import com.nguyenquyen.notificationservice.entity.Notification;
import com.nguyenquyen.notificationservice.entity.NotificationType;
import com.nguyenquyen.notificationservice.repository.ChatParticipantCacheRepository;
import com.nguyenquyen.notificationservice.repository.GroupChatMappingRepository;
import com.nguyenquyen.notificationservice.repository.NotificationRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final ChatParticipantCacheRepository chatParticipantCacheRepository;
    private final GroupChatMappingRepository groupChatMappingRepository;

    // ═══════════════════════════════════════════════════════════
    // NOTIFICATION CRUD
    // ═══════════════════════════════════════════════════════════

    /**
     * Persists a new notification for a user.
     *
     * <p>Called by Kafka consumers after processing an event.</p>
     *
     * @param userId        the recipient user's UUID
     * @param type          notification category
     * @param title         short title displayed in the notification list
     * @param body          optional longer body text
     * @param referenceId   UUID of the referenced resource (chatId / groupId)
     * @param referenceType type label for the reference ("CHAT" or "GROUP")
     */
    public void saveNotification(UUID userId,
                                 NotificationType type,
                                 String title,
                                 String body,
                                 UUID referenceId,
                                 String referenceType) {
        Notification notification = Notification.builder()
                .userId(userId)
                .type(type)
                .title(title)
                .body(body)
                .referenceId(referenceId)
                .referenceType(referenceType)
                .read(false)
                .build();

        notificationRepository.save(notification);
        log.debug("[NotificationService] Saved {} notification for user {}", type, userId);
    }


    @Transactional(readOnly = true)
    public Page<NotificationResponse> getMyNotifications(UUID userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return notificationRepository
                .findByUserIdOrderByCreatedAtDesc(userId, pageable)
                .map(NotificationResponse::from);
    }

    @Transactional(readOnly = true)
    public UnreadCountResponse getUnreadCount(UUID userId) {
        long count = notificationRepository.countByUserIdAndReadFalse(userId);
        return new UnreadCountResponse(count);
    }


    public void markAsRead(UUID notificationId, UUID userId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Notification not found: " + notificationId));

        if (!notification.getUserId().equals(userId)) {
            throw new AccessDeniedException("Notification does not belong to the requesting user");
        }

        notification.setRead(true);
        notificationRepository.save(notification);
        log.debug("[NotificationService] Marked notification {} as read for user {}", notificationId, userId);
    }

    public void markAllAsRead(UUID userId) {
        notificationRepository.markAllAsReadForUser(userId);
        log.debug("[NotificationService] Marked all notifications as read for user {}", userId);
    }


    public void deleteNotification(UUID notificationId, UUID userId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Notification not found: " + notificationId));

        if (!notification.getUserId().equals(userId)) {
            throw new AccessDeniedException("Notification does not belong to the requesting user");
        }

        notificationRepository.delete(notification);
        log.debug("[NotificationService] Deleted notification {} for user {}", notificationId, userId);
    }

    // ═══════════════════════════════════════════════════════════
    // PARTICIPANT CACHE MANAGEMENT (called by Kafka consumers)
    // ═══════════════════════════════════════════════════════════


    public void handleChatCreated(ChatCreatedEvent event) {
        UUID chatId = event.getChatId();

        // Store groupId → chatId mapping for GROUP chats
        if ("GROUP".equals(event.getType()) && event.getGroupId() != null) {
            if (!groupChatMappingRepository.existsByGroupId(event.getGroupId())) {
                groupChatMappingRepository.save(
                        GroupChatMapping.builder()
                                .groupId(event.getGroupId())
                                .chatId(chatId)
                                .build()
                );
                log.debug("[NotificationService] Mapped group {} → chat {}", event.getGroupId(), chatId);
            }
        }

        // Populate participant cache (idempotent per entry)
        if (event.getParticipantIds() != null) {
            for (UUID participantId : event.getParticipantIds()) {
                addParticipantIfAbsent(chatId, participantId);
            }
        }
    }

    @Transactional(readOnly = true)
    public Optional<UUID> getChatIdByGroupId(UUID groupId) {
        return groupChatMappingRepository.findByGroupId(groupId)
                .map(GroupChatMapping::getChatId);
    }

    @Transactional(readOnly = true)
    public List<UUID> getParticipantsByChatId(UUID chatId) {
        return chatParticipantCacheRepository.findByChatId(chatId)
                .stream()
                .map(ChatParticipantCache::getUserId)
                .toList();
    }

    public void addParticipantIfAbsent(UUID chatId, UUID userId) {
        if (!chatParticipantCacheRepository.existsByChatIdAndUserId(chatId, userId)) {
            chatParticipantCacheRepository.save(
                    ChatParticipantCache.builder()
                            .chatId(chatId)
                            .userId(userId)
                            .build()
            );
        }
    }

    public void removeParticipant(UUID chatId, UUID userId) {
        chatParticipantCacheRepository.deleteByChatIdAndUserId(chatId, userId);
        log.debug("[NotificationService] Removed participant {} from chat cache {}", userId, chatId);
    }

    public void removeAllParticipantsForChat(UUID chatId) {
        chatParticipantCacheRepository.deleteAllByChatId(chatId);
        log.debug("[NotificationService] Cleared all participants from chat cache {}", chatId);
    }
}
