package com.nguyenquyen.common.kafka.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class    UserEvent implements BaseEvent {

    private String type;       // UserEventType name
    private String actorId;    // Who performed the action
    private String recipientId; // Who is affected

    // User profile fields (for USER_CREATED / USER_UPDATED)
    private String userId;
    private String username;
    private String email;
    private String displayName;
    private String description;
    private String imageId;
    private String bannerImageId;

    // Notification preview
    private String previewText;
    private String previewImageId;

    @Builder.Default
    private Instant timestamp = Instant.now();
}
