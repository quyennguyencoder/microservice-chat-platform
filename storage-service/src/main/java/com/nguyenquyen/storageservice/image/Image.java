package com.nguyenquyen.storageservice.image;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;



@Entity
@Table(name = "image", indexes = {
        @Index(name = "idx_image_status", columnList = "status"),
        @Index(name = "idx_image_category_status", columnList = "category, status"),
        @Index(name = "idx_image_created_at", columnList = "createdAt")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class Image {

    @Id
    @Column(unique = true, nullable = false, length = 36)
    private String id;

    @Column(nullable = false)
    private String objectName;

    @Column(nullable = false)
    private String bucketName;

    private String fileName;
    private String contentType;
    private Long fileSize;
    private String category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ImageStatus status;

    private String thumbnailImageId;

    @Column(nullable = false)
    private Integer width;

    @Column(nullable = false)
    private Integer height;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;

    private Instant confirmedAt;

    public enum ImageStatus {
        PENDING,
        CONFIRMED,
        EXPIRED,
        DELETED
    }

    public boolean isPending() {
        return status == ImageStatus.PENDING;
    }

    public boolean isConfirmed() {
        return status == ImageStatus.CONFIRMED;
    }
}