package com.nguyenquyen.notificationservice.consumer;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.nguyenquyen.notificationservice.event.ChatCreatedEvent;
import com.nguyenquyen.notificationservice.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ChatEventConsumer {

    private final NotificationService notificationService;
    private final ObjectMapper        objectMapper;


    @KafkaListener(
        topics          = "chat-events",
        groupId         = "notification-service",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleChatEvent(String message) {
        try {
            ChatCreatedEvent event = objectMapper.readValue(message, ChatCreatedEvent.class);
            log.debug("[ChatEventConsumer] Received ChatCreatedEvent: chatId={} type={} groupId={}",
                    event.getChatId(), event.getType(), event.getGroupId());

            notificationService.handleChatCreated(event);

        } catch (Exception e) {
            log.error("[ChatEventConsumer] Failed to process chat-event message: {}", e.getMessage(), e);
            // Swallow the exception — Kafka consumer must not fail here;
            // bad messages are logged and skipped (no DLQ in this MVP).
        }
    }
}
