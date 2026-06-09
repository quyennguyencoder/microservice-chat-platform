package com.nguyenquyen.conversationservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * DTO representing a single participant in a chat.
 *
 * <p>Contains the participant's user UUID and the time they joined.
 * To resolve the user's name, avatar, or status, query the User Service
 * using the {@code userId} field.</p>
 *
 * @author  Aniket Kamlesh
 * @version 1.0.0
 * @since   1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParticipantResponse {

    /** UUID of the participating user. Resolve details via User Service. */
    private UUID userId;

    /** Timestamp when this user joined the chat. */
    private Instant joinedAt;
}
