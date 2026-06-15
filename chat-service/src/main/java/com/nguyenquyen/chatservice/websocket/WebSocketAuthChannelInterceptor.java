package com.nguyenquyen.chatservice.websocket;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.stereotype.Component;
import com.nguyenquyen.chatservice.presence.PresenceService;

import java.security.Principal;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class WebSocketAuthChannelInterceptor implements ChannelInterceptor {

    private final JwtDecoder jwtDecoder;
    private final WebSocketSessionManager sessionManager;
    private final PresenceService presenceService;

    @Override
    public Message<?> preSend(@NonNull Message<?> message, @NonNull MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(
                message, StompHeaderAccessor.class);

        if (accessor == null) {
            return message;
        }

        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            handleConnect(accessor);
        } else if (StompCommand.DISCONNECT.equals(accessor.getCommand())) {
            handleDisconnect(accessor);
        }

        if (accessor.isModified()) {
            return org.springframework.messaging.support.MessageBuilder.createMessage(message.getPayload(),
                    accessor.getMessageHeaders());
        }

        return message;
    }

    private void handleConnect(StompHeaderAccessor accessor) {
        List<String> authorization = accessor.getNativeHeader("Authorization");

        if (authorization == null || authorization.isEmpty()) {
            log.warn("No Authorization header in WebSocket CONNECT");
            return;
        }

        String token = authorization.get(0);
        if (token.startsWith("Bearer ")) {
            token = token.substring(7);
        }

        try {
            Jwt jwt = jwtDecoder.decode(token);
            String userId = jwt.getSubject();
            String sessionId = accessor.getSessionId();

            Principal userPrincipal = () -> userId;
            accessor.setUser(userPrincipal);

            sessionManager.registerSession(sessionId, userId);
            presenceService.updatePresence(userId);

            log.info("WebSocket connected: userId={}, sessionId={}", userId, sessionId);

        } catch (Exception e) {
            log.error("Failed to authenticate WebSocket connection", e);
        }
    }

    private void handleDisconnect(StompHeaderAccessor accessor) {
        String sessionId = accessor.getSessionId();

        sessionManager.getUserId(sessionId).ifPresent(userId -> {
            presenceService.removePresence(userId);
            sessionManager.removeSession(sessionId);
            log.info("WebSocket disconnected: userId={}, sessionId={}", userId, sessionId);
        });
    }
}
