package com.nguyenquyen.conversationservice.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Request DTO for creating a new private (1-on-1) chat.
 *
 * <p>The authenticated user's ID is taken from the {@code X-User-Id} header
 * injected by the API Gateway. This DTO only needs the target user's ID.</p>
 *
 * @author  Aniket Kamlesh
 * @version 1.0.0
 * @since   1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreatePrivateChatRequest {

    /**
     * UUID of the user to start a private chat with.
     * Must not be the same as the authenticated user's ID.
     */
    @NotNull(message = "Target user ID must not be null")
    private UUID targetUserId;
}
