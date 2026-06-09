package com.nguyenquyen.statusservice.dto.response;


import com.nguyenquyen.statusservice.entity.Status;
import com.nguyenquyen.statusservice.entity.UserStatus;
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
public class UserStatusResponse {

    /** The user's UUID. */
    private UUID userId;

    /** Current presence status. */
    private Status status;

    /**
     * Timestamp of the most recent status change.
     * May be {@code null} if the status was read from the Redis cache
     * and the DB fallback was skipped.
     */
    private Instant updatedAt;

    /**
     * Converts a {@link UserStatus} entity to a response DTO.
     *
     * @param u the entity
     * @return the response DTO
     */
    public static UserStatusResponse from(UserStatus u) {
        return UserStatusResponse.builder()
                .userId(u.getUserId())
                .status(u.getStatus())
                .updatedAt(u.getUpdatedAt())
                .build();
    }

    /**
     * Creates a default OFFLINE response for a user with no status record.
     *
     * @param userId the user's UUID
     * @return OFFLINE response with no timestamp
     */
    public static UserStatusResponse offline(UUID userId) {
        return UserStatusResponse.builder()
                .userId(userId)
                .status(Status.OFFLINE)
                .updatedAt(null)
                .build();
    }
}
