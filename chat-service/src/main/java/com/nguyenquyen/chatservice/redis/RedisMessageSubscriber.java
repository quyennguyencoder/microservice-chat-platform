package com.nguyenquyen.chatservice.redis;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.nguyenquyen.chatservice.websocket.WebSocketSessionManager;

@Service
@Slf4j
public class RedisMessageSubscriber implements MessageListener {

    private final ObjectMapper objectMapper;
    private final SimpMessagingTemplate messagingTemplate;
    private final WebSocketSessionManager sessionManager;
    private final GenericJackson2JsonRedisSerializer serializer;

    @Autowired
    public RedisMessageSubscriber(
            SimpMessagingTemplate messagingTemplate,
            WebSocketSessionManager sessionManager) {

        // Tự khởi tạo ObjectMapper cục bộ cho Redis, giống hệt cấu hình lúc Publish
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        this.objectMapper.activateDefaultTyping(
            this.objectMapper.getPolymorphicTypeValidator(),
            ObjectMapper.DefaultTyping.NON_FINAL
        );

        this.messagingTemplate = messagingTemplate;
        this.sessionManager = sessionManager;
        this.serializer = new GenericJackson2JsonRedisSerializer(this.objectMapper);
    }

    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            // Deserialize byte array -> RedisChatMessage object
            Object deserialized = serializer.deserialize(message.getBody());

            // Fix ClassCastException if Redis template serialized it as LinkedHashMap
            RedisChatMessage redisMessage = objectMapper.convertValue(deserialized, RedisChatMessage.class);

            if (redisMessage == null || redisMessage.getMessage() == null) {
                return;
            }

            String channel = new String(message.getChannel());
            log.trace("Received message from Redis channel: {}", channel);

            if (RedisMessagePublisher.CHAT_CHANNEL.equals(channel)) {
                handleChatMessage(redisMessage);
            } else if (RedisMessagePublisher.PRESENCE_CHANNEL.equals(channel)) {
                handlePresenceMessage(redisMessage);
            }

        } catch (Exception e) {
            log.error("Error processing message from Redis", e);
        }
    }

    private void handleChatMessage(RedisChatMessage redisMessage) {
        String recipientId = redisMessage.getRecipientId();

        // CHỈ GỬI nếu user này ĐANG CONNECT vào instance này (có session ở RAM local)
        if (recipientId != null && sessionManager.isUserConnectedLocally(recipientId)) {
            log.debug("User {} is connected to this instance. Forwarding message.", recipientId);
            messagingTemplate.convertAndSend("/queue/chat/" + recipientId, redisMessage.getMessage());
        }
    }

    private void handlePresenceMessage(RedisChatMessage redisMessage) {
        // Presence (Online/Offline) thì gửi broadcast cho TẤT CẢ các user đang connect vào instance này
        messagingTemplate.convertAndSend("/topic/presence", redisMessage.getMessage());
    }
}
