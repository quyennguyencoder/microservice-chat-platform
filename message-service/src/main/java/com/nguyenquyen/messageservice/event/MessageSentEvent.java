package com.nguyenquyen.messageservice.event;

import com.nguyenquyen.messageservice.entity.MessageType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageSentEvent {

    /** UUID of the new message. */
    private UUID messageId;

    /** UUID of the chat the message belongs to. */
    private UUID chatId;

    /** UUID of the user who sent the message. */
    private UUID senderId;

    /** Brief content preview (first 100 chars). Used in push notification text. */
    private String contentPreview;

    /** Type of message: TEXT, IMAGE, or FILE. */
    private MessageType type;

    /** Timestamp when the message was created. */
    private LocalDateTime createdAt;
}
