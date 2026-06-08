package com.nguyenquyen.contactservice.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

/**
 * Request body for sending a new contact request.
 *
 * @author  Aniket Kamlesh
 * @version 1.0.0
 * @since   1.0.0
 */
@Data
public class SendContactRequest {

    /**
     * UUID of the user to send the contact request to.
     * Must not be null. Cannot be the same as the requester's own userId.
     */
    @NotNull(message = "addresseeId is required")
    private UUID addresseeId;
}
