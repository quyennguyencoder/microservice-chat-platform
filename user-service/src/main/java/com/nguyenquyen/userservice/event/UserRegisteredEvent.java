package com.nguyenquyen.userservice.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Kafka Event — Consumed from "user-events" topic
 *
 * CONCEPT: Event Contract (Schema)
 * ──────────────────────────────────
 * This is a COPY of Auth Service's UserRegisteredEvent.
 * Both services must agree on the JSON structure.
 *
 * In production: use Apache Avro + Schema Registry for versioned schemas.
 * For now: plain JSON with matching field names is sufficient.
 *
 * Deserialization config (application.yml):
 *   spring.json.use.type.headers: false  → Ignore @class header from producer
 *   spring.json.value.default.type: ...UserRegisteredEvent → Always map to this class
 *
 * This way, even if Auth Service uses a different package name, JSON fields match.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserRegisteredEvent {

    private String eventType;       // "USER_REGISTERED"
    private String userId;          // Auth service UUID (will become profile.id)
    private String email;
    private String name;            // Full name (used as displayName)
    private String phone;
    private Instant registeredAt;
}
