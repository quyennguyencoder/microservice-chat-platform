package com.nguyenquyen.notificationservice.notification;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "notifications", indexes = {
        @Index(name = "idx_notification_recipient", columnList = "recipientId"),
        @Index(name = "idx_notification_recipient_status", columnList = "recipientId, status"),
        @Index(name = "idx_notification_recipient_updated", columnList = "recipientId, updatedAt DESC"),
        @Index(name = "idx_notification_aggregation", columnList = "recipientId, type, referenceId, status"),
        @Index(name = "idx_notification_follow_aggregation", columnList = "recipientId, type, status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String recipientId;

    @Column(nullable = false)
    private String actorId;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "notification_actors",
            joinColumns = @JoinColumn(name = "notification_id"),
            indexes = @Index(name = "idx_notification_actors_actor", columnList = "actor_id")
    )
    @Column(name = "actor_id")
    @OrderColumn(name = "position")
    @Builder.Default
    private List<String> recentActorIds = new ArrayList<>();

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
            name = "notification_all_actors",
            joinColumns = @JoinColumn(name = "notification_id"),
            indexes = @Index(name = "idx_notification_all_actors", columnList = "notification_id, actor_id")
    )
    @Column(name = "actor_id")
    @Builder.Default
    private Set<String> allActorIds = new HashSet<>();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private NotificationStatus status = NotificationStatus.UNREAD;

    @Column(nullable = false)
    @Builder.Default
    private Integer aggregatedCount = 1;

    @Column(nullable = false)
    @Builder.Default
    private Integer uniqueActorCount = 1;

    private UUID referenceId;
    private UUID secondaryRefId;

    @Column(length = 200)
    private String previewText;

    @Column(length = 200)
    private String previewImageId;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private Instant updatedAt;

    private Instant readAt;

    public void aggregate(String newActorId, String newPreviewText,
                          String newPreviewImageId, UUID newSecondaryRefId) {
        boolean isNewActor = addActor(newActorId);
        this.aggregatedCount++;
        if (isNewActor) {
            this.uniqueActorCount++;
        }
        if (newPreviewText != null) {
            this.previewText = newPreviewText;
        }
        if (newPreviewImageId != null) {
            this.previewImageId = newPreviewImageId;
        }
        if (this.type == NotificationType.NEW_MESSAGE && newSecondaryRefId != null) {
            this.secondaryRefId = newSecondaryRefId;
        }
    }

    private boolean addActor(String newActorId) {
        if (newActorId == null) {
            return false;
        }
        if (isSingleActionType()) {
            if (allActorIds.contains(newActorId)) {
                return false;
            }
            allActorIds.add(newActorId);
        }
        if (newActorId.equals(this.actorId)) {
            return false;
        }
        boolean isNewActor = !recentActorIds.contains(newActorId);
        recentActorIds.remove(newActorId);
        recentActorIds.addFirst(this.actorId);
        this.actorId = newActorId;
        if (recentActorIds.size() > 3) {
            recentActorIds = new ArrayList<>(recentActorIds.subList(0, 3));
        }
        return isNewActor || isSingleActionType();
    }

    public boolean canAggregateWith(String newActorId, NotificationType type) {
        if (isRepeatableActionType(type)) {
            return true;
        }
        if (allActorIds.contains(newActorId)) {
            return false;
        }
        return !this.actorId.equals(newActorId);
    }

    private boolean isRepeatableActionType(NotificationType type) {
        return type == NotificationType.NEW_MESSAGE;
    }

    private boolean isSingleActionType() {
        return type == NotificationType.GROUP_INVITE ||
                type == NotificationType.MEMBER_REMOVED ||
                type == NotificationType.FRIEND_REQUEST ||
                type == NotificationType.FRIEND_ACCEPTED;
    }

    public boolean containsActor(String actorId) {
        if (this.actorId.equals(actorId)) {
            return true;
        }
        if (isSingleActionType()) {
            return allActorIds.contains(actorId);
        }
        return recentActorIds.contains(actorId);
    }
}