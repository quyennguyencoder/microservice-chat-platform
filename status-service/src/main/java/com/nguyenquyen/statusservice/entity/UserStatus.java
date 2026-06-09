package com.nguyenquyen.statusservice.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;


@Entity
@Table(name = "user_status")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserStatus {

    /**
     * The user's UUID — acts as primary key.
     * Matches the auth-service userId (same UUID used across all services).
     */
    @Id
    @Column(name = "user_id")
    private UUID userId;

    /** The user's last set status. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Status status;

    /** Timestamp of the most recent status change. */
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
