package com.nguyenquyen.messageservice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;


@Entity
@Table(
    name = "messages",
    indexes = {
        @Index(name = "idx_message_chat_id",    columnList = "chat_id"),
        @Index(name = "idx_message_sender_id",  columnList = "sender_id"),
        @Index(name = "idx_message_created_at", columnList = "created_at")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Message {

    /** Auto-generated UUID primary key. */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * UUID of the chat this message belongs to.
     * References Chat Service's {@code chats.id} — no DB-level FK (cross-service).
     */
    @Column(name = "chat_id", nullable = false)
    private UUID chatId;

    /**
     * UUID of the user who sent this message.
     * Sourced from {@code X-User-Id} header injected by the API Gateway.
     */
    @Column(name = "sender_id", nullable = false)
    private UUID senderId;

    /**
     * The message content.
     * For TEXT: the message text.
     * For IMAGE/FILE: the media URL (resolved via Media Service — future).
     */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    /**
     * Type of message content: TEXT, IMAGE, or FILE.
     * Stored as a string for readability in the database.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MessageType type;

    /**
     * Delivery/read status of this message.
     * Starts as SENT, transitions to DELIVERED then READ.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MessageStatus status;

    /** Timestamp when the message was created. Never updated. */
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /** Timestamp when the message was last updated (e.g., status change). */
    @Column(name = "updated_at")
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
        if (this.status == null) {
            this.status = MessageStatus.SENT;
        }
        if (this.type == null) {
            this.type = MessageType.TEXT;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }
}
