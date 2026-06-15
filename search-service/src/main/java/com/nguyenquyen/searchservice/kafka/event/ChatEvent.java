package com.nguyenquyen.searchservice.kafka.event;

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
public class ChatEvent {

    private String type;
    private String actorId;
    private String recipientId;
    private String previewText;
    private String previewImageId;

    private UUID chatId;
    private UUID messageId;
    private String messageType;

    @Builder.Default
    private Instant timestamp = Instant.now();
}
