package com.nguyenquyen.userservice.entity;

import com.nguyenquyen.userservice.enums.OnlineStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.Instant;
import java.util.UUID;

/**
 * UserProfile Entity — user_db.user_profiles table
 *
 * ════════════════════════════════════════════════════════════
 * CONCEPT: Auth Service vs User Service — Separation of Concerns
 * ════════════════════════════════════════════════════════════
 *
 * Auth Service (auth_db.users):
 *   id, email, passwordHash, googleId, role, emailVerified, enabled
 *   → "WHO is this person? Are they authenticated?"
 *
 * User Service (user_db.user_profiles):
 *   id, displayName, username, bio, avatarUrl, onlineStatus, ...
 *   → "WHAT does this person's profile look like?"
 *
 * The `id` field is the SAME UUID in both services.
 * Auth service creates it → Kafka event → User service stores same id.
 * No foreign key across services (different databases) — just UUID convention.
 *
 * ════════════════════════════════════════════════════════════
 * CONCEPT: id NOT auto-generated
 * ════════════════════════════════════════════════════════════
 * Unlike auth service where UUID is auto-generated,
 * here we SET the id to match auth service userId.
 * This is why we use @GenerationType NONE (no @GeneratedValue).
 * The Kafka consumer sets profile.setId(UUID.fromString(event.getUserId()))
 */
@Entity
@Table(
    name = "user_profiles",
    indexes = {
        @Index(name = "idx_profiles_username", columnList = "username"),
        @Index(name = "idx_profiles_email",    columnList = "email")
    }
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfile {

    /**
     * Same UUID as auth_db.users.id — the link between both services.
     * NOT auto-generated — set from Kafka UserRegisteredEvent.userId.
     */
    @Id
    @Column(updatable = false, nullable = false)
    private UUID id;

    /**
     * Display name shown in chat (e.g. "Aniket Kamlesh")
     * Initialized from auth event `name` field.
     */
    @Column(nullable = false, length = 100)
    private String displayName;

    /**
     * Unique @handle (e.g. "aniket.kamlesh")
     * Auto-generated from email prefix on profile creation.
     * User can change it later (must stay unique).
     */
    @Column(unique = true, nullable = false, length = 50)
    private String username;

    /**
     * Denormalized copy of email from auth service.
     * Stored here for display and search — source of truth is auth service.
     */
    @Column(nullable = false, length = 255)
    private String email;

    /** Short bio / about me text */
    @Column(length = 500)
    private String bio;

    /** Profile picture URL (stored externally — S3, Cloudinary, etc.) */
    @Column(length = 1000)
    private String avatarUrl;

    /** Phone number (optional, for display) */
    @Column(length = 20)
    private String phone;

    /** Date of birth (optional, used for age display) */
    private LocalDate dateOfBirth;

    /** City, Country or custom location string */
    @Column(length = 100)
    private String location;

    /** Personal website or social link */
    @Column(length = 255)
    private String website;

    /**
     * Online/Offline/Away/Busy status
     * Updated when user connects/disconnects WebSocket (future).
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private OnlineStatus onlineStatus = OnlineStatus.OFFLINE;

    /** Timestamp of last active WebSocket connection close */
    private Instant lastSeen;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
        if (onlineStatus == null) {
            onlineStatus = OnlineStatus.OFFLINE;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
}
