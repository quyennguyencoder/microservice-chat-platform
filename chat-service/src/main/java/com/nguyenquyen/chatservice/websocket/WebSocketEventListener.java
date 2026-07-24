package com.nguyenquyen.chatservice.websocket;

import com.nguyenquyen.chatservice.redis.RedisMessagePublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import com.nguyenquyen.common.kafka.event.ChatEvent;
import com.nguyenquyen.chatservice.kafka.ChatEventPublisher;
import com.nguyenquyen.common.kafka.event.ChatEventType;

import java.security.Principal;

@Component
@RequiredArgsConstructor
@Slf4j
public class WebSocketEventListener {

    private final WebSocketSessionManager sessionManager;
    private final RedisMessagePublisher redisMessagePublisher;
    private final ChatEventPublisher eventPublisher;

    @EventListener
    public void handleWebSocketConnectListener(SessionConnectedEvent event) {
        Principal principal = event.getUser();
        if (principal != null) {
            String userId = principal.getName();
            log.info("User connected: {}", userId);

            broadcastPresenceChange(userId, true);
        }
    }

    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        String sessionId = event.getSessionId();

        sessionManager.getUserId(sessionId).ifPresent(userId -> {
            log.info("User disconnected: {}", userId);

            broadcastPresenceChange(userId, false);
        });
    }

    private void broadcastPresenceChange(String userId, boolean online) {
        WsOutgoingMessage message = online
                ? WsOutgoingMessage.userOnline(userId)
                : WsOutgoingMessage.userOffline(userId);

        // Phát sóng trạng thái Online/Offline lên Redis để tất cả Server đều biết
        redisMessagePublisher.broadcastPresence(message);

        eventPublisher.publish(ChatEvent.builder()
                .type(online ? ChatEventType.USER_ONLINE.name() : ChatEventType.USER_OFFLINE.name())
                .actorId(userId)
                .build());
    }
}
