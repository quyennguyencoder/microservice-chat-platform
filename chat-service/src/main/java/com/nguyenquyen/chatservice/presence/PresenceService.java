package com.nguyenquyen.chatservice.presence;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PresenceService {

    private final RedisTemplate<String, Object> redisTemplate;

    private static final String PRESENCE_KEY_PREFIX = "presence:user:";
    private static final String LAST_SEEN_KEY_PREFIX = "lastseen:user:";
    private static final String ACTIVE_CHAT_KEY_PREFIX = "active-chat:user:";
    private static final String TYPING_KEY_PREFIX = "typing:chat:";

    @Value("${presence.online-threshold-seconds:60}")
    private int onlineThresholdSeconds;

    @Value("${presence.typing-timeout-seconds:3}")
    private int typingTimeoutSeconds;

    public void updatePresence(String userId) {
        Instant now = Instant.now();

        String presenceKey = PRESENCE_KEY_PREFIX + userId;
        redisTemplate.opsForValue().set(presenceKey, now.toEpochMilli());
        redisTemplate.expire(presenceKey, Duration.ofSeconds(onlineThresholdSeconds));

        String lastSeenKey = LAST_SEEN_KEY_PREFIX + userId;
        redisTemplate.opsForValue().set(lastSeenKey, now.toEpochMilli());
        redisTemplate.expire(lastSeenKey, Duration.ofDays(30));

        log.debug("Presence updated for user: {}", userId);
    }

    public void removePresence(String userId) {
        redisTemplate.delete(PRESENCE_KEY_PREFIX + userId);
        redisTemplate.delete(ACTIVE_CHAT_KEY_PREFIX + userId);

        String lastSeenKey = LAST_SEEN_KEY_PREFIX + userId;
        redisTemplate.opsForValue().set(lastSeenKey, Instant.now().toEpochMilli());
        redisTemplate.expire(lastSeenKey, Duration.ofDays(30));

        log.debug("Presence removed for user: {}", userId);
    }

    public boolean isUserOnline(String userId) {
        return redisTemplate.hasKey(PRESENCE_KEY_PREFIX + userId);
    }

    public Optional<Instant> getLastSeen(String userId) {
        Object value = redisTemplate.opsForValue().get(LAST_SEEN_KEY_PREFIX + userId);
        if (value == null) {
            return Optional.empty();
        }
        try {
            long timestamp = value instanceof Long ? (Long) value : Long.parseLong(value.toString());
            return Optional.of(Instant.ofEpochMilli(timestamp));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    public PresenceStatus getPresenceStatus(String userId) {
        if (isUserOnline(userId)) {
            return PresenceStatus.ONLINE;
        }

        Optional<Instant> lastSeen = getLastSeen(userId);
        if (lastSeen.isEmpty()) {
            return PresenceStatus.LONG_AGO;
        }

        return calculateStatus(lastSeen.get());
    }

    public UserPresenceResponse.UserPresence getUserPresence(String userId) {
        boolean isOnline = isUserOnline(userId);
        Instant lastSeen = getLastSeen(userId).orElse(null);
        PresenceStatus status = isOnline ? PresenceStatus.ONLINE
                : (lastSeen != null ? calculateStatus(lastSeen) : PresenceStatus.LONG_AGO);

        return new UserPresenceResponse.UserPresence(status, lastSeen, isOnline);
    }

    public UserPresenceResponse getPresenceData(Set<String> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return new UserPresenceResponse(Collections.emptyMap());
        }

        Map<String, UserPresenceResponse.UserPresence> presenceData = userIds.stream()
                .collect(Collectors.toMap(
                        userId -> userId,
                        this::getUserPresence));

        return new UserPresenceResponse(presenceData);
    }

    public Map<String, Boolean> getOnlineStatus(Set<String> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Collections.emptyMap();
        }

        return userIds.stream()
                .collect(Collectors.toMap(
                        userId -> userId,
                        this::isUserOnline));
    }

    private PresenceStatus calculateStatus(Instant lastSeen) {
        Instant now = Instant.now();
        Duration timeSince = Duration.between(lastSeen, now);

        if (timeSince.toMinutes() < 5) {
            return PresenceStatus.RECENTLY;
        }

        if (timeSince.toHours() < 1) {
            return PresenceStatus.LAST_HOUR;
        }

        LocalDate lastSeenDate = lastSeen.atZone(ZoneId.systemDefault()).toLocalDate();
        LocalDate today = LocalDate.now(ZoneId.systemDefault());

        if (lastSeenDate.equals(today)) {
            return PresenceStatus.TODAY;
        }

        if (lastSeenDate.equals(today.minusDays(1))) {
            return PresenceStatus.YESTERDAY;
        }

        if (timeSince.toDays() <= 7) {
            return PresenceStatus.WEEK;
        }

        return PresenceStatus.LONG_AGO;
    }

    public void setActiveChat(String userId, UUID chatId) {
        String key = ACTIVE_CHAT_KEY_PREFIX + userId;
        redisTemplate.opsForValue().set(key, chatId.toString());
        log.debug("User {} entered chat {}", userId, chatId);
    }

    public void removeActiveChat(String userId) {
        redisTemplate.delete(ACTIVE_CHAT_KEY_PREFIX + userId);
        log.debug("User {} left active chat", userId);
    }

    public Optional<UUID> getActiveChat(String userId) {
        Object value = redisTemplate.opsForValue().get(ACTIVE_CHAT_KEY_PREFIX + userId);
        if (value == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(UUID.fromString(value.toString()));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    public boolean isUserInChat(String userId, UUID chatId) {
        return getActiveChat(userId)
                .map(activeChatId -> activeChatId.equals(chatId))
                .orElse(false);
    }

    public void startTyping(String userId, UUID chatId) {
        String key = TYPING_KEY_PREFIX + chatId;
        redisTemplate.opsForSet().add(key, userId);
        redisTemplate.expire(key, Duration.ofSeconds(typingTimeoutSeconds));
        log.debug("User {} started typing in chat {}", userId, chatId);
    }

    public void stopTyping(String userId, UUID chatId) {
        String key = TYPING_KEY_PREFIX + chatId;
        redisTemplate.opsForSet().remove(key, userId);
        log.debug("User {} stopped typing in chat {}", userId, chatId);
    }

    public Set<String> getTypingUsers(UUID chatId) {
        String key = TYPING_KEY_PREFIX + chatId;
        Set<Object> members = redisTemplate.opsForSet().members(key);
        if (members == null) {
            return Collections.emptySet();
        }
        return members.stream()
                .map(Object::toString)
                .collect(Collectors.toSet());
    }
}