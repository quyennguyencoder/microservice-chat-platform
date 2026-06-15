package com.nguyenquyen.notificationservice.counter;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class UnreadCounterService {

    private final RedisTemplate<String, Object> redisTemplate;

    private static final String UNREAD_COUNT_KEY_PREFIX = "notification:unread:";

    public void increment(String userId) {
        String key = UNREAD_COUNT_KEY_PREFIX + userId;
        redisTemplate.opsForValue().increment(key);
        log.debug("Unread counter incremented for user: {}", userId);
    }

    public void decrement(String userId, int count) {
        String key = UNREAD_COUNT_KEY_PREFIX + userId;
        Long current = getCount(userId);
        if (current != null && current > 0) {
            long newValue = Math.max(0, current - count);
            redisTemplate.opsForValue().set(key, newValue);
            log.debug("Unread counter decremented for user {}: {} -> {}", userId, current, newValue);
        }
    }

    public void reset(String userId) {
        String key = UNREAD_COUNT_KEY_PREFIX + userId;
        redisTemplate.opsForValue().set(key, 0L);
        log.debug("Unread counter reset for user: {}", userId);
    }

    public Long getCount(String userId) {
        String key = UNREAD_COUNT_KEY_PREFIX + userId;
        Object value = redisTemplate.opsForValue().get(key);
        if (value == null) {
            return null;
        }
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        try {
            return Long.parseLong(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public void setCount(String userId, long count) {
        String key = UNREAD_COUNT_KEY_PREFIX + userId;
        redisTemplate.opsForValue().set(key, count);
        log.debug("Unread counter set for user {}: {}", userId, count);
    }
}