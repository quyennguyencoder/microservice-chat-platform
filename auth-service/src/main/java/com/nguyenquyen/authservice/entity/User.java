package com.nguyenquyen.authservice.entity;

import com.nguyenquyen.authservice.enums.Role;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * User Entity — auth_db.users table
 *
 * CONCEPT: Auth vs Profile separation
 * ─────────────────────────────────────
 * Auth Service User entity has only authentication-related fields:
 *   - email, passwordHash, emailVerified, googleId, role
 *
 * Profile-related fields (bio, profilePicture, status message) are in User Profile
 * Service (8082). Both are linked by UUID.
 *
 * Why separate?
 *   - Auth Service responsibility: "Who is this user? Are they valid?"
 *   - Profile Service responsibility: "What is this user's profile?"
 *   - Separation of concerns — changing one service won't affect the other
 */
@Entity
@Table(
    name = "users",
    indexes = {
        @Index(name = "idx_users_email", columnList = "email"),
        @Index(name = "idx_users_google_id", columnList = "googleId")
    }
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @Column(unique = true, nullable = false, length = 255)
    private String email;

    // Nullable — Google OAuth users don't have a password
    @Column(length = 60)    // BCrypt always produces 60-char hash
    private String passwordHash;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 20)
    private String phone;

    @Column(nullable = false)
    @Builder.Default
    private boolean emailVerified = false;

    // Nullable — only set for Google OAuth users
    @Column(unique = true, length = 255)
    private String googleId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private Role role = Role.USER;

    // CONCEPT: Soft disable — banned users ke liye false set karo
    // Hard delete dangerous hai — foreign keys, audit trails
    @Column(nullable = false)
    @Builder.Default
    private boolean enabled = true;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
