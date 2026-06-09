package com.nguyenquyen.statusservice.service;


import com.nguyenquyen.statusservice.dto.response.UserStatusResponse;
import com.nguyenquyen.statusservice.entity.Status;
import com.nguyenquyen.statusservice.entity.UserStatus;
import com.nguyenquyen.statusservice.repository.UserStatusRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class StatusService {

    private final UserStatusRepository userStatusRepository;
    private final StringRedisTemplate  stringRedisTemplate;

    public static final String REDIS_KEY_PREFIX   = "status:";
    public static final long   ONLINE_TTL_MINUTES = 5L;

    // ═══════════════════════════════════════════════════════════
    // UPDATE STATUS
    // ═══════════════════════════════════════════════════════════

    public void updateMyStatus(UUID userId, Status status) {
        // 1. Upsert in PostgreSQL
        UserStatus record = userStatusRepository.findById(userId)
                .orElse(UserStatus.builder().userId(userId).build());
        record.setStatus(status);
        record.setUpdatedAt(Instant.now());
        userStatusRepository.save(record);

        // 2. Write to Redis cache
        String key = REDIS_KEY_PREFIX + userId;
        if (status == Status.ONLINE) {
            // ONLINE → TTL-based: auto-expires to OFFLINE if heartbeat stops
            stringRedisTemplate.opsForValue()
                    .set(key, status.name(), Duration.ofMinutes(ONLINE_TTL_MINUTES));
        } else {
            // OFFLINE / AWAY / BUSY → stored without TTL (explicit change)
            stringRedisTemplate.opsForValue().set(key, status.name());
        }

        log.debug("[StatusService] Updated status for user {} → {}", userId, status);
    }

    // ═══════════════════════════════════════════════════════════
    // HEARTBEAT
    // ═══════════════════════════════════════════════════════════

    public void heartbeat(UUID userId) {
        String key = REDIS_KEY_PREFIX + userId;

        // Refresh or set ONLINE with a fresh TTL
        stringRedisTemplate.opsForValue()
                .set(key, Status.ONLINE.name(), Duration.ofMinutes(ONLINE_TTL_MINUTES));

        // Ensure DB is also ONLINE (idempotent update)
        UserStatus record = userStatusRepository.findById(userId)
                .orElse(UserStatus.builder().userId(userId).build());

        if (record.getStatus() != Status.ONLINE) {
            record.setStatus(Status.ONLINE);
            record.setUpdatedAt(Instant.now());
            userStatusRepository.save(record);
        }

        log.debug("[StatusService] Heartbeat received from user {}", userId);
    }

    // ═══════════════════════════════════════════════════════════
    // GET STATUS
    // ═══════════════════════════════════════════════════════════

    @Transactional(readOnly = true)
    public UserStatusResponse getStatus(UUID userId) {
        // 1. Try Redis cache
        String key          = REDIS_KEY_PREFIX + userId;
        String cachedStatus = stringRedisTemplate.opsForValue().get(key);

        if (cachedStatus != null) {
            try {
                Status status = Status.valueOf(cachedStatus);
                // Augment with updatedAt from DB (best-effort; null if not persisted yet)
                Instant updatedAt = userStatusRepository.findById(userId)
                        .map(UserStatus::getUpdatedAt)
                        .orElse(null);
                return UserStatusResponse.builder()
                        .userId(userId)
                        .status(status)
                        .updatedAt(updatedAt)
                        .build();
            } catch (IllegalArgumentException e) {
                log.warn("[StatusService] Unexpected value in Redis for {}: {}", userId, cachedStatus);
            }
        }

        // 2. Fallback to PostgreSQL
        return userStatusRepository.findById(userId)
                .map(UserStatusResponse::from)
                .orElse(UserStatusResponse.offline(userId));
    }

    @Transactional(readOnly = true)
    public List<UserStatusResponse> getBatchStatus(List<UUID> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return List.of();
        }

        // Pre-fetch DB records for all users at once (avoids N+1)
        Map<UUID, UserStatus> dbMap = userStatusRepository.findAllByUserIdIn(userIds)
                .stream()
                .collect(Collectors.toMap(UserStatus::getUserId, u -> u));

        return userIds.stream()
                .map(userId -> resolveStatus(userId, dbMap))
                .collect(Collectors.toList());
    }

    // ─── Private helpers ─────────────────────────────────────────

    private UserStatusResponse resolveStatus(UUID userId, Map<UUID, UserStatus> dbMap) {
        String key          = REDIS_KEY_PREFIX + userId;
        String cachedStatus = stringRedisTemplate.opsForValue().get(key);

        if (cachedStatus != null) {
            try {
                Status status = Status.valueOf(cachedStatus);
                Instant updatedAt = dbMap.containsKey(userId)
                        ? dbMap.get(userId).getUpdatedAt() : null;
                return UserStatusResponse.builder()
                        .userId(userId)
                        .status(status)
                        .updatedAt(updatedAt)
                        .build();
            } catch (IllegalArgumentException e) {
                log.warn("[StatusService] Unexpected value in Redis for {}: {}", userId, cachedStatus);
            }
        }

        return dbMap.containsKey(userId)
                ? UserStatusResponse.from(dbMap.get(userId))
                : UserStatusResponse.offline(userId);
    }
}
