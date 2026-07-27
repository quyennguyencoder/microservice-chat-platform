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
import java.util.concurrent.CopyOnWriteArraySet;

@Component
@RequiredArgsConstructor
@Slf4j
public class SseEmitterManager {

    private final Map<String, CopyOnWriteArraySet<SseEmitter>> emitters = new ConcurrentHashMap<>();

    @Value("${sse.connection-timeout-ms:3600000}")
    private long connectionTimeout;

    public SseEmitter createEmitter(String userId) {
        SseEmitter emitter = new SseEmitter(connectionTimeout);

        emitter.onCompletion(() -> removeSpecificEmitter(userId, emitter));
        emitter.onTimeout(() -> removeSpecificEmitter(userId, emitter));
        emitter.onError(ex -> removeSpecificEmitter(userId, emitter));

        emitters.computeIfAbsent(userId, k -> new CopyOnWriteArraySet<>()).add(emitter);
        log.info("SSE connection created for user: {} (Total connections: {})", userId, emitters.get(userId).size());

        // Send connected event only to this new emitter
        try {
            SseEvent connectedEvent = SseEvent.connected(userId);
            emitter.send(SseEmitter.event()
                    .name(connectedEvent.getType())
                    .data(connectedEvent));
        } catch (IOException e) {
            removeSpecificEmitter(userId, emitter);
        }

        return emitter;
    }

    private void removeSpecificEmitter(String userId, SseEmitter emitter) {
        var userEmitters = emitters.get(userId);
        if (userEmitters != null) {
            userEmitters.remove(emitter);
            if (userEmitters.isEmpty()) {
                emitters.remove(userId);
            }
        }
    }

    public void sendToUser(String userId, SseEvent event) {
        var userEmitters = emitters.get(userId);
        if (userEmitters == null || userEmitters.isEmpty()) {
            log.debug("No active SSE connection for user: {}", userId);
            return;
        }

        for (SseEmitter emitter : userEmitters) {
            try {
                emitter.send(SseEmitter.event()
                        .name(event.getType())
                        .data(event));
                log.debug("SSE event sent to user {}: type={}", userId, event.getType());
            } catch (IOException e) {
                log.warn("Failed to send SSE event to user {}: {}", userId, e.getMessage());
                removeSpecificEmitter(userId, emitter);
            }
        }
    }

    public void broadcast(SseEvent event) {
        emitters.keySet().forEach(userId -> sendToUser(userId, event));
    }

    public void removeEmitter(String userId) {
        var userEmitters = emitters.remove(userId);
        if (userEmitters != null) {
            for (SseEmitter emitter : userEmitters) {
                try {
                    emitter.complete();
                } catch (Exception e) {
                    log.debug("Error completing emitter for user {}: {}", userId, e.getMessage());
                }
            }
            log.info("All SSE connections removed for user: {}", userId);
        }
    }

    public boolean hasConnection(String userId) {
        return emitters.containsKey(userId) && !emitters.get(userId).isEmpty();
    }

    public int getActiveConnectionCount() {
        return emitters.values().stream().mapToInt(CopyOnWriteArraySet::size).sum();
    }

    @Scheduled(fixedRateString = "${sse.heartbeat-interval-ms:30000}")
    public void sendHeartbeat() {
        if (emitters.isEmpty()) {
            return;
        }

        SseEvent heartbeat = SseEvent.heartbeat();
        emitters.keySet().forEach(userId -> sendToUser(userId, heartbeat));
        log.debug("Heartbeat sent to {} connections", getActiveConnectionCount());
    }
}