package com.nguyenquyen.userservice.friendship;

public record FriendshipCheckResponse(
        String userId,
        FriendshipStatus status,
        boolean isFriend,
        boolean isPendingIncoming,
        boolean isPendingOutgoing,
        boolean isBlocked
) {
}
