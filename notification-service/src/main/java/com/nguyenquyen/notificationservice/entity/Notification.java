package com.nguyenquyen.notificationservice.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;


@Entity
@Table(
    name = "notifications",
    indexes = {
        @Index(name = "idx_notification_user_id",         columnList = "user_id"),
        @Index(name = "idx_notification_user_id_is_read", columnList = "user_id, is_read"),
        @Index(name = "idx_notification_created_at",      columnList = "created_at")
    }
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Notification {

    /** UUID primary key — auto-generated. */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** The user this notification belongs to. */
    @Column(name = "user_id", nullable = false)
    private UUID userId;

    /** Notification category — drives the UI icon and wording. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private NotificationType type;

    /** Short title displayed in the notification list (e.g. "New message in your chat"). */
    @Column(nullable = false, length = 200)
    private String title;

    /** Optional longer body text (e.g. message content preview). */
    @Column(length = 500)
    private String body;

    /**
     * UUID of the resource that triggered this notification.
     * For NEW_MESSAGE → chatId; for GROUP_* → groupId.
     */
    @Column(name = "reference_id")
    private UUID referenceId;

    /**
     * Type of the referenced resource ("CHAT" or "GROUP").
     * Helps the frontend build a deep-link to the relevant screen.
     */
    @Column(name = "reference_type", length = 20)
    private String referenceType;

    /** Whether the user has seen / acknowledged this notification. */
    @Column(name = "is_read", nullable = false)
    @Builder.Default
    private boolean read = false;

    /** Timestamp when this notification was created (immutable). */
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}
