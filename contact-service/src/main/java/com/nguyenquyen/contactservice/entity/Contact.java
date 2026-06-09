package com.nguyenquyen.contactservice.entity;

import com.nguyenquyen.contactservice.enums.ContactStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;


@Entity
@Table(
    name = "contacts",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_contact_requester_addressee",
        columnNames = {"requester_id", "addressee_id"}
    )
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Contact {

    /** Auto-generated UUID primary key. */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * UUID of the user who initiated the contact request.
     * Sourced from the {@code X-User-Id} header injected by the API Gateway.
     */
    @Column(name = "requester_id", nullable = false)
    private UUID requesterId;

    /**
     * UUID of the user who received the contact request.
     */
    @Column(name = "addressee_id", nullable = false)
    private UUID addresseeId;

    /**
     * Current status of the contact relationship.
     * Stored as a string for readability in the database.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ContactStatus status;

    /** Timestamp when the contact request was created. Never updated. */
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /** Timestamp when the contact status was last updated. */
    @Column(name = "updated_at")
    private Instant updatedAt;

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
