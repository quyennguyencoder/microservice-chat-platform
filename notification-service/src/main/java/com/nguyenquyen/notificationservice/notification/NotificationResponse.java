package com.nguyenquyen.notificationservice.notification;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record NotificationResponse(
        UUID id,
        String recipientId,
        String actorId,
        List<String> recentActorIds,
        NotificationType type,
        NotificationStatus status,
        Integer aggregatedCount,
        Integer uniqueActorCount,
        UUID referenceId,
        UUID secondaryRefId,
        String previewText,
        String previewImageId,
        Instant createdAt,
        Instant updatedAt,
        Instant readAt
) {}