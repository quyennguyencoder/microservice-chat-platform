package com.nguyenquyen.notificationservice.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nguyenquyen.notificationservice.event.MessageSentEvent;
import com.nguyenquyen.notificationservice.entity.NotificationType;
import com.nguyenquyen.notificationservice.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class MessageEventConsumer {

    private final NotificationService notificationService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "message-events", groupId = "notification-service", containerFactory = "kafkaListenerContainerFactory")
    public void handleMessageEvent(String message) {
        try {
            MessageSentEvent event = objectMapper.readValue(message, MessageSentEvent.class);
            log.debug("[MessageEventConsumer] Received MessageSentEvent: chatId={} senderId={}",
                    event.getChatId(), event.getSenderId());

            UUID chatId = event.getChatId();
            UUID senderId = event.getSenderId();

            // Look up participants from local cache
            List<UUID> participants = notificationService.getParticipantsByChatId(chatId);
            if (participants.isEmpty()) {
                log.warn("[MessageEventConsumer] No participants found in cache for chatId={}. " +
                        "Notification skipped (chat-events may not have been processed yet).", chatId);
                return;
            }

            // Build notification content from the event
            String body = event.getContentPreview();
            if (body != null && body.length() > 150) {
                body = body.substring(0, 150) + "…";
            }

            // Notify every participant except the sender
            String finalBody = body;
            participants.stream()
                    .filter(participantId -> !participantId.equals(senderId))
                    .forEach(recipientId -> notificationService.saveNotification(
                            recipientId,
                            NotificationType.NEW_MESSAGE,
                            "New message in your chat",
                            finalBody,
                            chatId,
                            "CHAT"));

            log.debug("[MessageEventConsumer] Created NEW_MESSAGE notifications for {} recipients in chat {}",
                    participants.size() - 1, chatId);

        } catch (Exception e) {
            log.error("[MessageEventConsumer] Failed to process message-event: {}", e.getMessage(), e);
        }
    }
}
