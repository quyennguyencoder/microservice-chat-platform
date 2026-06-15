package com.nguyenquyen.chatservice.chat;

import java.time.Instant;

public record ChatMemberResponse(
        String userId,
        ChatMemberRole role,
        Instant joinedAt
) {}
