package com.nguyenquyen.notificationservice.kafka.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.Instant;
import java.util.UUID;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public abstract class BaseEvent {

    private String type;
    private String actorId;
    private String recipientId;
    private UUID referenceId;
    private String previewText;
    private String previewImageId;
    private Instant timestamp;
}