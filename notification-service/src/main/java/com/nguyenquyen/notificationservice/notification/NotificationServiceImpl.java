package com.nguyenquyen.notificationservice.notification;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.nguyenquyen.notificationservice.counter.UnreadCounterService;
import com.nguyenquyen.common.exception.ResourceNotFoundException;
import com.nguyenquyen.common.kafka.event.BaseEvent;
import com.nguyenquyen.common.kafka.event.ChatEvent;

import com.nguyenquyen.notificationservice.sse.SseEmitterManager;
import com.nguyenquyen.notificationservice.sse.SseEvent;
import com.nguyenquyen.common.util.SecurityUtils;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationMapper notificationMapper;
    private final NotificationAggregationStrategy aggregationStrategy;
    private final SseEmitterManager sseEmitterManager;
    private final UnreadCounterService unreadCounterService;

    @Override
    public void createAndSendNotification(BaseEvent event) {
        if (event.getActorId().equals(event.getRecipientId())) {
            log.debug("Skipping self-notification for user {}", event.getActorId());
            return;
        }

        NotificationType type = mapEventType(event);

        Optional<Notification> existingOpt = aggregationStrategy.findForAggregation(event, type);

        Notification notification;
        boolean isNewNotification;

        if (existingOpt.isPresent()) {
            Notification existing = existingOpt.get();

            if (existing.containsActor(event.getActorId()) && isSingleActionType(type)) {
                log.debug("Duplicate {} from {} for {}, skipping",
                        type, event.getActorId(), event.getRecipientId());
                return;
            }

            if (aggregationStrategy.shouldAggregate(existing, event, type)) {
                notification = existing;
                notification.aggregate(
                        event.getActorId(),
                        event.getPreviewText(),
                        event.getPreviewImageId(),
                        aggregationStrategy.getSecondaryRefId(event, type)
                );

                isNewNotification = false;

                log.info("Notification aggregated: id={}, type={}, count={}, uniqueActors={}",
                        notification.getId(), type,
                        notification.getAggregatedCount(),
                        notification.getUniqueActorCount());
            } else {
                log.debug("Cannot aggregate {} from {}, already in notification",
                        type, event.getActorId());
                return;
            }
        } else {
            notification = buildNotification(event, type);
            isNewNotification = true;
        }

        Notification saved = notificationRepository.save(notification);

        if (isNewNotification) {
            unreadCounterService.increment(event.getRecipientId());
        }

        NotificationResponse response = notificationMapper.toResponse(saved);
        sseEmitterManager.sendToUser(
                event.getRecipientId(),
                isNewNotification
                        ? SseEvent.notification(response)
                        : SseEvent.notificationUpdated(response)
        );

        log.info("Notification {}: type={}, recipient={}, actor={}",
                isNewNotification ? "created" : "updated",
                type, event.getRecipientId(), event.getActorId());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<NotificationResponse> getMyNotifications(Pageable pageable) {
        String userId = SecurityUtils.requireCurrentUserId();
        return notificationRepository
                .findByRecipientIdOrderByUpdatedAtDesc(userId, pageable)
                .map(notificationMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<NotificationResponse> getMyUnreadNotifications(Pageable pageable) {
        String userId = SecurityUtils.requireCurrentUserId();
        return notificationRepository
                .findByRecipientIdAndStatusOrderByUpdatedAtDesc(
                        userId, NotificationStatus.UNREAD, pageable)
                .map(notificationMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public long getUnreadCount() {
        String userId = SecurityUtils.requireCurrentUserId();

        Long cachedCount = unreadCounterService.getCount(userId);
        if (cachedCount != null) {
            return cachedCount;
        }

        long count = notificationRepository.countByRecipientIdAndStatus(
                userId, NotificationStatus.UNREAD);

        unreadCounterService.setCount(userId, count);
        return count;
    }

    @Override
    public int markAllAsRead() {
        String userId = SecurityUtils.requireCurrentUserId();

        int updated = notificationRepository.markAllAsRead(
                userId,
                NotificationStatus.UNREAD,
                NotificationStatus.READ,
                Instant.now()
        );

        if (updated > 0) {
            unreadCounterService.reset(userId);
            sseEmitterManager.sendToUser(userId, SseEvent.unreadUpdate(0));
        }

        log.info("Marked {} notifications as read for user {}", updated, userId);
        return updated;
    }

    @Override
    public int markAsRead(List<UUID> ids) {
        String userId = SecurityUtils.requireCurrentUserId();

        int updated = notificationRepository.markAsRead(
                ids, userId, NotificationStatus.READ, Instant.now()
        );

        if (updated > 0) {
            unreadCounterService.decrement(userId, updated);
            long newCount = getUnreadCount();
            sseEmitterManager.sendToUser(userId, SseEvent.unreadUpdate(newCount));
        }

        log.info("Marked {} notifications as read for user {}", updated, userId);
        return updated;
    }

    @Override
    public void delete(UUID id) {
        String userId = SecurityUtils.requireCurrentUserId();

        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Notification not found: " + id));

        if (!notification.getRecipientId().equals(userId)) {
            throw new ResourceNotFoundException("Notification not found: " + id);
        }

        if (notification.getStatus() == NotificationStatus.UNREAD) {
            unreadCounterService.decrement(userId, 1);
        }

        notificationRepository.delete(notification);
        log.info("Notification {} deleted by user {}", id, userId);
    }

    @Scheduled(cron = "0 0 3 * * *")
    public void cleanupOldNotifications() {
        Instant threshold = Instant.now().minus(30, ChronoUnit.DAYS);
        int deleted = notificationRepository.deleteOlderThan(threshold);
        log.info("Cleaned up {} old notifications", deleted);
    }

    private boolean isSingleActionType(NotificationType type) {
        return type == NotificationType.GROUP_INVITE ||
                type == NotificationType.MEMBER_REMOVED ||
                type == NotificationType.FRIEND_REQUEST ||
                type == NotificationType.FRIEND_ACCEPTED;
    }

    private Notification buildNotification(BaseEvent event, NotificationType type) {
        Set<String> initialActors = new HashSet<>();
        initialActors.add(event.getActorId());

        Notification.NotificationBuilder builder = Notification.builder()
                .recipientId(event.getRecipientId())
                .actorId(event.getActorId())
                .type(type)
                .previewText(event.getPreviewText())
                .previewImageId(event.getPreviewImageId())
                .aggregatedCount(1)
                .uniqueActorCount(1)
                .allActorIds(initialActors);

        if (event instanceof ChatEvent chatEvent) {
            builder.referenceId(chatEvent.getChatId());
            builder.secondaryRefId(chatEvent.getMessageId());
        }

        return builder.build();
    }

    private NotificationType mapEventType(BaseEvent event) {
        return switch (event.getType()) {
            case "NEW_MESSAGE" -> NotificationType.NEW_MESSAGE;
            case "GROUP_CREATED", "GROUP_INVITE" -> NotificationType.GROUP_INVITE;
            case "MEMBER_ADDED" -> NotificationType.MEMBER_ADDED;
            case "MEMBER_REMOVED" -> NotificationType.MEMBER_REMOVED;
            case "FRIEND_REQUEST" -> NotificationType.FRIEND_REQUEST;
            case "FRIEND_ACCEPTED" -> NotificationType.FRIEND_ACCEPTED;
            default -> throw new IllegalArgumentException("Unknown event type: " + event.getType());
        };
    }
}
