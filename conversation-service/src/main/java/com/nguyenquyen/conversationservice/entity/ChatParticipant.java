package com.nguyenquyen.conversationservice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;


@Entity
@Table(
    name = "chat_participants",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_chat_participant",
        columnNames = {"chat_id", "user_id"}
    )
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatParticipant {

    /** Auto-generated UUID primary key. */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * The chat this participant belongs to.
     * FK: chat_participants.chat_id → chats.id
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chat_id", nullable = false)
    private Chat chat;

    /**
     * UUID of the participating user.
     * Sourced from the {@code X-User-Id} header injected by the API Gateway.
     * User details (name, avatar) are resolved via User Service.
     */
    @Column(name = "user_id", nullable = false)
    private UUID userId;

    /** Timestamp when this user joined the chat. */
    @Column(name = "joined_at", nullable = false, updatable = false)
    private LocalDateTime joinedAt;

    @PrePersist
    protected void onCreate() {
        this.joinedAt = LocalDateTime.now();
    }
}
