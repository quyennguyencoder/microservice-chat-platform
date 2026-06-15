package com.nguyenquyen.notificationservice.kafka.consumer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import com.nguyenquyen.notificationservice.kafka.event.ChatEvent;
import com.nguyenquyen.notificationservice.notification.NotificationService;
import java.util.Set;

@Component
@RequiredArgsConstructor
@Slf4j
public class ChatEventConsumer {

    private final NotificationService notificationService;

    @KafkaListener(
            topics = "${kafka.topics.chat-events:chat-events}",
            groupId = "notification-service",
            properties = {"spring.json.value.default.type=com.nguyenquyen.notificationservice.kafka.event.ChatEvent"}
    )
    public void consume(ChatEvent event) {
        log.info("Received chat event: type={}, chatId={}, sender={}, recipient={}",
                event.getType(), event.getChatId(), event.getActorId(), event.getRecipientId());

        try {
            if (event.getRecipientId() == null) {
                log.debug("Skipping event with null recipientId (likely intended for other services)");
                return;
            }

            if (Set.of("NEW_MESSAGE", "GROUP_CREATED", "MEMBER_ADDED", "MEMBER_REMOVED").contains(event.getType())) {
                notificationService.createAndSendNotification(event);
            }
        } catch (Exception e) {
            log.error("Error processing chat event", e);
        }
    }
}
