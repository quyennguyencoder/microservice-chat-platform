package com.nguyenquyen.chatservice.kafka;

import com.nguyenquyen.chatservice.chat.entity.Chat;
import com.nguyenquyen.chatservice.chat.service.ChatService;
import com.nguyenquyen.chatservice.message.dto.MessageResponse;
import com.nguyenquyen.chatservice.message.entity.Message;
import com.nguyenquyen.chatservice.message.enums.MessageState;
import com.nguyenquyen.chatservice.message.mapper.MessageMapper;
import com.nguyenquyen.chatservice.message.repository.MessageRepository;
import com.nguyenquyen.chatservice.redis.RedisMessagePublisher;
import com.nguyenquyen.chatservice.websocket.WsOutgoingMessage;
import com.nguyenquyen.common.kafka.event.ModerationEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class ModerationEventConsumer {

    private final MessageRepository messageRepository;
    private final MessageMapper messageMapper;
    private final ChatService chatService;
    private final RedisMessagePublisher redisMessagePublisher;

    @KafkaListener(
            topics = "${kafka.topics.moderation-events:moderation-events}",
            groupId = "chat-service-moderation",
            properties = {"spring.json.value.default.type=com.nguyenquyen.common.kafka.event.ModerationEvent"}
    )
    @Transactional
    public void consume(ModerationEvent event) {
        log.info("Received moderation event: messageId={}, chatId={}, label={}, confidence={}, action={}",
                event.getMessageId(), event.getChatId(), event.getLabel(), event.getConfidence(), event.getAction());

        if (event.getMessageId() == null) {
            log.warn("ModerationEvent received without messageId");
            return;
        }

        // Chỉ xử lý nếu hành động là FLAGGED hoặc HIDDEN
        if (!"FLAGGED".equalsIgnoreCase(event.getAction()) && !"HIDDEN".equalsIgnoreCase(event.getAction())) {
            log.debug("No moderation action required for message {}", event.getMessageId());
            return;
        }

        messageRepository.findById(event.getMessageId()).ifPresentOrElse(message -> {
            message.setState(MessageState.FLAGGED);
            Message updated = messageRepository.save(message);
            MessageResponse response = messageMapper.toResponse(updated);

            Map<String, Object> payload = Map.of(
                    "messageId", event.getMessageId().toString(),
                    "label", event.getLabel(),
                    "confidence", event.getConfidence(),
                    "action", event.getAction()
            );

            WsOutgoingMessage wsMessage = WsOutgoingMessage.messageFlagged(
                    event.getChatId(),
                    response,
                    payload
            );

            // Bắn WebSocket qua Redis pub/sub cho tất cả các thành viên trong cuộc trò chuyện
            try {
                Chat chat = chatService.getChatEntityById(event.getChatId());
                for (String memberId : chat.getMemberIds()) {
                    redisMessagePublisher.sendToUser(memberId, wsMessage);
                }
                log.info("Message {} marked as FLAGGED and broadcasted to {} members",
                        event.getMessageId(), chat.getMemberIds().size());
            } catch (Exception e) {
                log.error("Error broadcasting flagged message to chat members: {}", e.getMessage(), e);
            }
        }, () -> log.warn("Message with id {} not found for moderation update", event.getMessageId()));
    }
}
