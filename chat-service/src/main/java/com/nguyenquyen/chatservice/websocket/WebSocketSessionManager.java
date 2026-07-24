package com.nguyenquyen.chatservice.websocket;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Slf4j
public class WebSocketSessionManager {

    // CHỈ lưu trữ kết nối đang nằm trên instance này.
    // Nếu app deploy 3 server, server nào chạy thì nó chỉ lưu map của server đó.

    // userId → sessionId
    private final Map<String, String> userSessions = new ConcurrentHashMap<>();

    // sessionId → userId
    private final Map<String, String> sessionUsers = new ConcurrentHashMap<>();

    public void registerSession(String sessionId, String userId) {
        userSessions.put(userId, sessionId);
        sessionUsers.put(sessionId, userId);
        log.debug("Local Session registered: userId={}, sessionId={}", userId, sessionId);
    }

    public void removeSession(String sessionId) {
        String userId = sessionUsers.remove(sessionId);
        if (userId != null) {
            userSessions.remove(userId);
            log.debug("Local Session removed: userId={}, sessionId={}", userId, sessionId);
        }
    }

    public Optional<String> getUserId(String sessionId) {
        return Optional.ofNullable(sessionUsers.get(sessionId));
    }

    public Optional<String> getSessionId(String userId) {
        return Optional.ofNullable(userSessions.get(userId));
    }

    /**
     * Dùng nội bộ để RedisMessageSubscriber kiểm tra xem user
     * có đang cắm kết nối trực tiếp vào máy chủ này hay không.
     */
    public boolean isUserConnectedLocally(String userId) {
        return userSessions.containsKey(userId);
    }
}