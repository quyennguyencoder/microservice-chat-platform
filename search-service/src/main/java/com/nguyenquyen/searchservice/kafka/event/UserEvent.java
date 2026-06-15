package com.nguyenquyen.searchservice.kafka.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserEvent {

    private String type;
    private String actorId;
    private String recipientId;

    // User data for indexing
    private String userId;
    private String username;
    private String email;
    private String description;
    private String imageId;
    private String bannerImageId;


    private String previewText;
    private String previewImageId;

    @Builder.Default
    private Instant timestamp = Instant.now();
}