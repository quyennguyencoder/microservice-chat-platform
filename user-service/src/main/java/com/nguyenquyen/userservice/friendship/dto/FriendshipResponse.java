package com.nguyenquyen.userservice.friendship.dto;

import com.nguyenquyen.userservice.friendship.enums.FriendshipStatus;

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
