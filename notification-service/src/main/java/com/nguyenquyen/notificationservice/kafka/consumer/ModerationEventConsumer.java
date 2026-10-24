package com.nguyenquyen.notificationservice.kafka.consumer;

import com.nguyenquyen.common.kafka.event.ModerationEvent;
import com.nguyenquyen.notificationservice.notification.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ModerationEventConsumer {

    private final NotificationService notificationService;

    @KafkaListener(
            topics = "${kafka.topics.moderation-events:moderation-events}",
            groupId = "notification-service-moderation",
            properties = {"spring.json.value.default.type=com.nguyenquyen.common.kafka.event.ModerationEvent"}
    )
    public void consume(ModerationEvent event) {
        log.info("NotificationService received moderation event: messageId={}, sender={}, label={}, action={}",
                event.getMessageId(), event.getSenderId(), event.getLabel(), event.getAction());

        if (event.getSenderId() == null) {
            log.debug("Skipping moderation warning: senderId is null");
            return;
        }

        if ("FLAGGED".equalsIgnoreCase(event.getAction()) || "HIDDEN".equalsIgnoreCase(event.getAction())) {
            try {
                notificationService.sendModerationWarning(
                        event.getSenderId(),
                        event.getLabel(),
                        event.getMessageId()
                );
            } catch (Exception e) {
                log.error("Failed to send moderation warning notification: {}", e.getMessage(), e);
            }
        }
    }
}
