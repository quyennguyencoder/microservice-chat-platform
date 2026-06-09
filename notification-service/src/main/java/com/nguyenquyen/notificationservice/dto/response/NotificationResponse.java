package com.nguyenquyen.notificationservice.dto.response;


import com.nguyenquyen.notificationservice.entity.Notification;
import com.nguyenquyen.notificationservice.entity.NotificationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationResponse {

    private UUID id;
    private NotificationType type;
    private String title;
    private String body;
    private UUID referenceId;
    private String referenceType;
    private boolean read;
    private Instant createdAt;

    /**
     * Converts a {@link Notification} entity to a {@link NotificationResponse} DTO.
     *
     * @param n the entity to convert
     * @return the response DTO
     */
    public static NotificationResponse from(Notification n) {
        return NotificationResponse.builder()
                .id(n.getId())
                .type(n.getType())
                .title(n.getTitle())
                .body(n.getBody())
                .referenceId(n.getReferenceId())
                .referenceType(n.getReferenceType())
                .read(n.isRead())
                .createdAt(n.getCreatedAt())
                .build();
    }
}
