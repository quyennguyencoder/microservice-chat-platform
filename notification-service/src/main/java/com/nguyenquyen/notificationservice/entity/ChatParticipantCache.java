package com.nguyenquyen.notificationservice.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Table(
    name = "chat_participant_cache",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_chat_participant",
        columnNames = {"chat_id", "user_id"}
    )
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatParticipantCache {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** The chat this participant belongs to. */
    @Column(name = "chat_id", nullable = false)
    private UUID chatId;

    /** The participating user's UUID. */
    @Column(name = "user_id", nullable = false)
    private UUID userId;
}
