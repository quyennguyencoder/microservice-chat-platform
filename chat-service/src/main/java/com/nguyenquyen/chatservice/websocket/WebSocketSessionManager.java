package com.nguyenquyen.chatservice.websocket;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Slf4j
public class WebSocketSessionManager {

    // userId → sessionId
    private final Map<String, String> userSessions = new ConcurrentHashMap<>();

    // sessionId → userId
    private final Map<String, String> sessionUsers = new ConcurrentHashMap<>();

    // chatId → Set<userId>
    private final Map<UUID, Set<String>> chatUsers = new ConcurrentHashMap<>();

    public void registerSession(String sessionId, String userId) {
        userSessions.put(userId, sessionId);
        sessionUsers.put(sessionId, userId);
        log.debug("Session registered: userId={}, sessionId={}", userId, sessionId);
    }

    public void removeSession(String sessionId) {
        String userId = sessionUsers.remove(sessionId);
        if (userId != null) {
            userSessions.remove(userId);

            // Удаляем из всех чатов
            chatUsers.values().forEach(users -> users.remove(userId));

            log.debug("Session removed: userId={}, sessionId={}", userId, sessionId);
        }
    }

    public Optional<String> getUserId(String sessionId) {
        return Optional.ofNullable(sessionUsers.get(sessionId));
    }

    public Optional<String> getSessionId(String userId) {
        return Optional.ofNullable(userSessions.get(userId));
    }

    public void joinChat(String userId, UUID chatId) {
        chatUsers.computeIfAbsent(chatId, k -> ConcurrentHashMap.newKeySet())
                .add(userId);
        log.debug("User {} joined chat {}", userId, chatId);
    }

    public void leaveChat(String userId, UUID chatId) {
        Set<String> users = chatUsers.get(chatId);
        if (users != null) {
            users.remove(userId);
            if (users.isEmpty()) {
                chatUsers.remove(chatId);
            }
        }
        log.debug("User {} left chat {}", userId, chatId);
    }

    public Set<String> getChatUsers(UUID chatId) {
        return chatUsers.getOrDefault(chatId, Set.of());
    }

    public boolean isUserInChat(String userId, UUID chatId) {
        Set<String> users = chatUsers.get(chatId);
        return users != null && users.contains(userId);
    }

    public Set<String> getOnlineUsers() {
        return Set.copyOf(userSessions.keySet());
    }

    public boolean isUserOnline(String userId) {
        return userSessions.containsKey(userId);
    }
}