package com.nguyenquyen.notificationservice.counter;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import com.nguyenquyen.common.constant.RedisConstants;

@Service
@RequiredArgsConstructor
@Slf4j
public class UnreadCounterService {

    private final RedisTemplate<String, Object> redisTemplate;

    private static final String UNREAD_COUNT_KEY_PREFIX = RedisConstants.UNREAD_COUNT_KEY_PREFIX;

    public void increment(String userId) {
        String key = UNREAD_COUNT_KEY_PREFIX + userId;
        redisTemplate.opsForValue().increment(key);
        log.debug("Unread counter incremented for user: {}", userId);
    }

    public void decrement(String userId, int count) {
        String key = UNREAD_COUNT_KEY_PREFIX + userId;
        Long newValue = redisTemplate.opsForValue().increment(key, -count);
        if (newValue != null && newValue < 0) {
            redisTemplate.opsForValue().set(key, 0L);
            newValue = 0L;
        }
        log.debug("Unread counter decremented for user {}: new value -> {}", userId, newValue);
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