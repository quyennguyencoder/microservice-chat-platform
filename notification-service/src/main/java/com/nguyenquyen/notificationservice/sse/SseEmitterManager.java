package com.nguyenquyen.notificationservice.sse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
@Slf4j
public class SseEmitterManager {

    private final Map<String, SseEmitter> emitters = new ConcurrentHashMap<>();

    @Value("${sse.connection-timeout-ms:3600000}")
    private long connectionTimeout;

    public SseEmitter createEmitter(String userId) {
        removeEmitter(userId);

        SseEmitter emitter = new SseEmitter(connectionTimeout);

        emitter.onCompletion(() -> {
            log.debug("SSE completed for user: {}", userId);
            emitters.remove(userId);
        });

        emitter.onTimeout(() -> {
            log.debug("SSE timeout for user: {}", userId);
            emitters.remove(userId);
        });

        emitter.onError(ex -> {
            log.debug("SSE error for user {}: {}", userId, ex.getMessage());
            emitters.remove(userId);
        });

        emitters.put(userId, emitter);
        log.info("SSE connection created for user: {}", userId);

        sendToUser(userId, SseEvent.connected(userId));

        return emitter;
    }

    public void sendToUser(String userId, SseEvent event) {
        SseEmitter emitter = emitters.get(userId);
        if (emitter == null) {
            log.debug("No active SSE connection for user: {}", userId);
            return;
        }

        try {
            emitter.send(SseEmitter.event()
                    .name(event.getType())
                    .data(event));
            log.debug("SSE event sent to user {}: type={}", userId, event.getType());
        } catch (IOException e) {
            log.warn("Failed to send SSE event to user {}: {}", userId, e.getMessage());
            removeEmitter(userId);
        }
    }

    public void broadcast(SseEvent event) {
        emitters.keySet().forEach(userId -> sendToUser(userId, event));
    }

    public void removeEmitter(String userId) {
        SseEmitter emitter = emitters.remove(userId);
        if (emitter != null) {
            try {
                emitter.complete();
            } catch (Exception e) {
                log.debug("Error completing emitter for user {}: {}", userId, e.getMessage());
            }
            log.info("SSE connection removed for user: {}", userId);
        }
    }

    public boolean hasConnection(String userId) {
        return emitters.containsKey(userId);
    }

    public int getActiveConnectionCount() {
        return emitters.size();
    }

    @Scheduled(fixedRateString = "${sse.heartbeat-interval-ms:30000}")
    public void sendHeartbeat() {
        if (emitters.isEmpty()) {
            return;
        }

        SseEvent heartbeat = SseEvent.heartbeat();
        emitters.keySet().forEach(userId -> sendToUser(userId, heartbeat));
        log.debug("Heartbeat sent to {} connections", emitters.size());
    }
}