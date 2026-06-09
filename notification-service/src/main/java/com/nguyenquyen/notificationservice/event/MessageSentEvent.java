package com.nguyenquyen.notificationservice.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;


@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class MessageSentEvent {

    /** UUID of the message that was sent. */
    private UUID messageId;

    /** UUID of the chat the message belongs to. */
    private UUID chatId;

    /** UUID of the user who sent the message (excluded from notifications). */
    private UUID senderId;

    /** First 100 characters of the message content — used as notification body. */
    private String contentPreview;

    /** Message type: "TEXT", "IMAGE", or "FILE". */
    private String type;

    /** Timestamp when the message was created. */
    private Instant createdAt;
}
