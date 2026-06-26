package com.nguyenquyen.notificationservice.notification;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import com.nguyenquyen.common.kafka.event.BaseEvent;
import com.nguyenquyen.common.kafka.event.ChatEvent;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationAggregationStrategy {

    private final NotificationRepository notificationRepository;

    public Optional<Notification> findForAggregation(BaseEvent event, NotificationType type) {
        String recipientId = event.getRecipientId();

        return switch (type) {
            case NEW_MESSAGE -> {
                ChatEvent chatEvent = (ChatEvent) event;
                yield notificationRepository.findUnreadMessagesNotification(
                        recipientId,
                        chatEvent.getChatId()
                );
            }
            default -> Optional.empty();
        };
    }

    public boolean shouldAggregate(Notification existing, BaseEvent event, NotificationType type) {
        if (existing == null) {
            return false;
        }

        String actorId = event.getActorId();
        return existing.canAggregateWith(actorId, type);
    }

    public boolean isDuplicate(Notification existing, BaseEvent event, NotificationType type) {
        return false;
    }

    public UUID getSecondaryRefId(BaseEvent event, NotificationType type) {
        if (event instanceof ChatEvent chatEvent) {
            return chatEvent.getMessageId();
        }
        return null;
    }
}
