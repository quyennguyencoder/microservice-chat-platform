package com.nguyenquyen.authservice.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Kafka Event — Published when a new user completes email verification
 *
 * CONCEPT: Event-Driven Architecture
 * ─────────────────────────────────────
 * Topic: "user-events"
 *
 * Who publishes: Auth Service (after OTP verification)
 * Who consumes:  User Profile Service (8082) — creates default profile
 *                Notification Service (8086) — sends welcome email (future)
 *
 * Why Kafka vs direct REST call?
 * ─────────────────────────────
 * Auth Service calls User Profile Service directly (REST):
 *   - Tight coupling — Auth knows about Profile
 *   - If Profile Service is down → user registration fails
 *   - Hard to add new consumers later
 *
 * Kafka event:
 *   - Loose coupling — Auth only knows topic name
 *   - Profile Service down → event waits in Kafka, processed when service recovers
 *   - Add Notification Service later — no Auth code change needed
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserRegisteredEvent {

    private String eventType = "USER_REGISTERED";
    private String userId;
    private String email;
    private String name;
    private String phone;
    private LocalDateTime registeredAt;
}
