package com.nguyenquyen.messageservice.dto;


import com.nguyenquyen.messageservice.entity.MessageStatus;
import com.nguyenquyen.messageservice.entity.MessageType;
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
public class MessageResponse {

    /** Unique identifier of this message. */
    private UUID id;

    /** UUID of the chat this message belongs to. */
    private UUID chatId;

    /** UUID of the user who sent this message. Resolve via User Service. */
    private UUID senderId;

    /** The message content (text body or media URL). */
    private String content;

    /** Type of message: TEXT, IMAGE, or FILE. */
    private MessageType type;

    /** Delivery/read status: SENT, DELIVERED, or READ. */
    private MessageStatus status;

    /** Timestamp when the message was created. */
    private Instant createdAt;

    /** Timestamp when the message was last updated (e.g., status change). */
    private Instant updatedAt;
}
