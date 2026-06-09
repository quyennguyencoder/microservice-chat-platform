package com.nguyenquyen.messageservice.dto;

import com.nguyenquyen.messageservice.entity.MessageType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Request DTO for sending a new message.
 *
 * <p>The sender's ID is taken from the {@code X-User-Id} header injected
 * by the API Gateway. This DTO only needs the target chat and message content.</p>
 *
 * @author  Aniket Kamlesh
 * @version 1.0.0
 * @since   1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SendMessageRequest {

    /**
     * UUID of the chat to send the message to.
     * The sender must be a participant of this chat (validated by Chat Service).
     */
    @NotNull(message = "Chat ID must not be null")
    private UUID chatId;

    /**
     * The message content.
     * For TEXT type: the plain-text message body.
     * For IMAGE/FILE type: the URL of the uploaded media.
     */
    @NotBlank(message = "Message content must not be blank")
    @Size(max = 4000, message = "Message content must not exceed 4000 characters")
    private String content;

    /**
     * Type of the message content.
     * Defaults to TEXT if not provided.
     */
    private MessageType type = MessageType.TEXT;
}
