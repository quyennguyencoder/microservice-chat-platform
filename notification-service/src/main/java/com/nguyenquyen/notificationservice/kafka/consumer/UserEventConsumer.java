package com.nguyenquyen.notificationservice.kafka.consumer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import com.nguyenquyen.common.kafka.event.UserEvent;
import com.nguyenquyen.common.kafka.event.UserEventType;
import com.nguyenquyen.notificationservice.notification.NotificationService;

@Component
@RequiredArgsConstructor
@Slf4j
public class UserEventConsumer {

    private final NotificationService notificationService;

    @KafkaListener(
            topics = "${kafka.topics.user-events:user-events}",
            groupId = "notification-service",
            properties = {"spring.json.value.default.type=com.nguyenquyen.common.kafka.event.UserEvent"}
    )
    public void consume(UserEvent event) {
        log.info("Received user event: type={}, actor={}, recipient={}",
                event.getType(), event.getActorId(), event.getRecipientId());

        try {
            UserEventType type = UserEventType.valueOf(event.getType());
            if (type == UserEventType.FRIEND_REQUEST || type == UserEventType.FRIEND_ACCEPTED) {
                notificationService.createAndSendNotification(event);
            } else {
                log.debug("Skipping user event not meant for notifications: {}", type);
            }
        } catch (IllegalArgumentException e) {
            log.warn("Unknown user event type: {}", event.getType());
        } catch (Exception e) {
            log.error("Error processing user event", e);
            throw new RuntimeException("Re-throwing exception to trigger Kafka retry / DLQ", e);
        }
    }
}
