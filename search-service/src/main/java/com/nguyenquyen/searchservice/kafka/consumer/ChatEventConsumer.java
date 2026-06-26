package com.nguyenquyen.searchservice.kafka.consumer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import com.nguyenquyen.common.kafka.event.ChatEvent;
import com.nguyenquyen.common.kafka.event.ChatEventType;
import com.nguyenquyen.searchservice.message.MessageDocument;
import com.nguyenquyen.searchservice.message.MessageSearchService;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatEventConsumer {

    private final MessageSearchService messageSearchService;

    @KafkaListener(
            topics = "${kafka.topics.chat-events:chat-events}",
            groupId = "${spring.kafka.consumer.group-id:search-service-group}",
            properties = {"spring.json.value.default.type=com.nguyenquyen.common.kafka.event.ChatEvent"}
    )
    public void consumeChatEvent(ChatEvent event) {
        log.debug("Consumed chat event: type={}, chatId={}, messageId={}",
                event.getType(), event.getChatId(), event.getMessageId());

        try {
            ChatEventType type = ChatEventType.valueOf(event.getType());

            switch (type) {
                case NEW_MESSAGE, MESSAGE_UPDATED -> handleNewMessage(event);
                case MESSAGE_DELETED -> handleMessageDeleted(event);
                default -> log.trace("Ignoring chat event type: {}", type);
            }

        } catch (IllegalArgumentException e) {
            log.warn("Unknown chat event type: {}", event.getType());
        } catch (Exception e) {
            log.error("Error processing chat event", e);
        }
    }

    private void handleNewMessage(ChatEvent event) {
        if (event.getMessageId() == null || event.getChatId() == null) {
            log.warn("Missing messageId or chatId in NEW_MESSAGE or MESSAGE_UPDATED event");
            return;
        }

        MessageDocument document = MessageDocument.builder()
                .messageId(event.getMessageId().toString())
                .chatId(event.getChatId().toString())
                .senderId(event.getActorId())
                .content(event.getPreviewText()) // Using preview text as content for indexing since we don't have the full message payload in the event
                .type(event.getMessageType())
                .createdAt(event.getTimestamp())
                .build();

        messageSearchService.indexMessage(document);
    }

    private void handleMessageDeleted(ChatEvent event) {
        if (event.getMessageId() == null) {
            log.warn("Missing messageId in MESSAGE_DELETED event");
            return;
        }

        messageSearchService.deleteMessage(event.getMessageId().toString());
    }
}
