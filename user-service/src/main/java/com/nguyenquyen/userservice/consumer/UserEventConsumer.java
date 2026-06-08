package com.nguyenquyen.userservice.consumer;

import com.nguyenquyen.userservice.event.UserRegisteredEvent;
import com.nguyenquyen.userservice.service.UserProfileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

/**
 * Kafka Consumer — Listens to "user-events" topic
 *
 * ════════════════════════════════════════════════════════════
 * CONCEPT: @KafkaListener
 * ════════════════════════════════════════════════════════════
 *
 * topics       → Which Kafka topic(s) to listen to
 * groupId      → Consumer group ID
 *   - All instances of this service belong to the SAME group
 *   - Kafka delivers each message to ONE instance in the group
 *   - This gives us horizontal scaling without duplicate processing
 *
 * CONCEPT: Consumer Group Offset
 * ───────────────────────────────
 * Kafka tracks which messages each consumer group has processed.
 * auto-offset-reset: earliest → If this group has never consumed before,
 * start from the BEGINNING (catch all historical messages).
 *
 * This is critical for first deploy: ensures profiles are created for
 * users who registered before the User Service was running.
 *
 * CONCEPT: At-Least-Once Delivery
 * ────────────────────────────────
 * Kafka guarantees each message is delivered AT LEAST ONCE.
 * If consumer crashes mid-processing → message re-delivered.
 * Service must be IDEMPOTENT — same event processed twice must be safe.
 *
 * UserProfileService.createProfileFromEvent() handles this:
 * → existsById() check before creating → skip if already exists.
 *
 * ════════════════════════════════════════════════════════════
 * CONCEPT: Deserialization
 * ════════════════════════════════════════════════════════════
 * Auth Service sends: JsonSerializer → JSON bytes
 * User Service reads: JsonDeserializer → UserRegisteredEvent POJO
 *
 * Config (application.yml):
 *   spring.json.use.type.headers: false → Ignore @class header
 *   spring.json.value.default.type: UserRegisteredEvent → Always deserialize to this
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class UserEventConsumer {

    private final UserProfileService profileService;

    @KafkaListener(
        topics = "user-events",
        groupId = "${spring.kafka.consumer.group-id:nguyenquyen.userservice}"
    )
    public void handleUserEvent(
            @Payload UserRegisteredEvent event,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset) {

        log.info("📨 Received Kafka event | topic={} partition={} offset={} eventType={}",
                topic, partition, offset, event.getEventType());

        try {
            if ("USER_REGISTERED".equals(event.getEventType())) {
                profileService.createProfileFromEvent(event);
            } else {
                log.warn("Unknown event type: {} — skipping", event.getEventType());
            }
        } catch (Exception e) {
            // Log but don't rethrow — prevents Kafka from re-delivering infinitely
            // In production: send to Dead Letter Topic (DLT) for manual review
            log.error("❌ Failed to process event for userId={}: {}",
                    event.getUserId(), e.getMessage(), e);
        }
    }
}
