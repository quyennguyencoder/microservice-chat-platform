package com.nguyenquyen.conversationservice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "chats")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Chat {

    /** Auto-generated UUID primary key. */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * Type of chat: PRIVATE (1-on-1) or GROUP (future).
     * Stored as a string for readability in the database.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ChatType type;

    /**
     * Optional reference to a Group in group-service.
     * Non-null for GROUP chats created via the group-events Kafka integration.
     * Used for idempotency checks and member sync lookups.
     */
    @Column(name = "group_id", unique = true)
    private UUID groupId;

    /** Timestamp when the chat was created. Never updated. */
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /** Timestamp when the chat was last updated (e.g., new participant added). */
    @Column(name = "updated_at")
    private Instant updatedAt;

    /**
     * All participants in this chat.
     * Eagerly fetched to include participants in every chat response.
     */
    @OneToMany(mappedBy = "chat", cascade = CascadeType.ALL, fetch = FetchType.EAGER, orphanRemoval = true)
    @Builder.Default
    private List<ChatParticipant> participants = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }
}
