package com.nguyenquyen.userservice.friendship.dto;

import com.nguyenquyen.userservice.friendship.enums.FriendshipStatus;

public record FriendshipCheckResponse(
        String userId,
        FriendshipStatus status,
        boolean isFriend,
        boolean isPendingIncoming,
        boolean isPendingOutgoing,
        boolean isBlocked
) {
}
