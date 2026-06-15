package com.nguyenquyen.searchservice.client;

import java.time.Instant;

public record FriendshipResponse(
        String userId,
        String username,
        String displayName,
        String imageId,
        String status,
        Instant since
) {}
