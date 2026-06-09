package com.nguyenquyen.contactservice.dto.response;

import com.nguyenquyen.contactservice.enums.ContactStatus;
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
public class ContactResponse {

    /** Unique identifier of this contact record. */
    private UUID id;

    /** UUID of the user who sent the contact request. */
    private UUID requesterId;

    /** UUID of the user who received the contact request. */
    private UUID addresseeId;

    /** Current status of the relationship: PENDING or ACCEPTED. */
    private ContactStatus status;

    /** Timestamp when the contact request was originally created. */
    private Instant createdAt;

    /** Timestamp of the last status change (e.g., when request was accepted). */
    private Instant updatedAt;
}
