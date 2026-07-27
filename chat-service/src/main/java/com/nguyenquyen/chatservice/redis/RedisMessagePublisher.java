package com.nguyenquyen.chatservice.redis;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import java.util.UUID;
import com.nguyenquyen.common.constant.RedisConstants;

@Service
@RequiredArgsConstructor
@Slf4j
public class RedisMessagePublisher {

    private final RedisTemplate<String, Object> redisTemplate;

    @Value("${spring.application.name:chat-service}")
    private String serverId;

    public static final String CHAT_CHANNEL = RedisConstants.CHAT_CHANNEL;
    public static final String PRESENCE_CHANNEL = RedisConstants.PRESENCE_CHANNEL;

    /**
     * Gửi tin nhắn cho một người dùng cụ thể thông qua Redis Pub/Sub
     */
    public void sendToUser(String recipientId, com.nguyenquyen.chatservice.websocket.WsOutgoingMessage message) {
        RedisChatMessage redisMessage = RedisChatMessage.builder()
                .recipientId(recipientId)
                .message(message)
                .sourceServerId(serverId)
                .build();

        redisTemplate.convertAndSend(CHAT_CHANNEL, redisMessage);
        log.debug("Published message for user {} to Redis channel", recipientId);
    }

    /**
     * Gửi tin nhắn broadcast (ví dụ: thông báo thay đổi trạng thái online)
     */
    public void broadcastPresence(com.nguyenquyen.chatservice.websocket.WsOutgoingMessage message) {
        RedisChatMessage redisMessage = RedisChatMessage.builder()
                .message(message)
                .sourceServerId(serverId)
                .build();

        redisTemplate.convertAndSend(PRESENCE_CHANNEL, redisMessage);
        log.debug("Published presence broadcast to Redis channel");
    }
}
