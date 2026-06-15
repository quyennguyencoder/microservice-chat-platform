package com.nguyenquyen.userservice.friendship;

import java.time.Instant;

public record FriendshipResponse(
        String userId,
        String username,
        String displayName,
        String imageId,
        FriendshipStatus status,
        Instant since
) {
}
