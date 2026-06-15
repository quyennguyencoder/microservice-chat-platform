package com.nguyenquyen.userservice.user;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class User {

    @Id
    @Column(unique = true, nullable = false)
    private String id; // Keycloak subject ID

    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @Column(unique = true, nullable = false, length = 100)
    private String email;

    @Column(name = "display_name", length = 100)
    private String displayName;

    @Column(name = "image_id", length = 100)
    private String imageId; // Avatar → storage-service

    @Column(name = "banner_image_id", length = 100)
    private String bannerImageId; // Banner → storage-service

    @Column(length = 500)
    private String description; // Bio

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private Instant updatedAt;
}