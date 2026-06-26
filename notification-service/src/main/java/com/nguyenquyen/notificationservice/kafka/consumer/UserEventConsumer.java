package com.nguyenquyen.notificationservice.kafka.consumer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import com.nguyenquyen.common.kafka.event.UserEvent;
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
            if ("FRIEND_REQUEST".equals(event.getType()) || "FRIEND_ACCEPTED".equals(event.getType())) {
                notificationService.createAndSendNotification(event);
            } else {
                log.debug("Skipping user event not meant for notifications: {}", event.getType());
            }
        } catch (Exception e) {
            log.error("Error processing user event", e);
        }
    }
}
