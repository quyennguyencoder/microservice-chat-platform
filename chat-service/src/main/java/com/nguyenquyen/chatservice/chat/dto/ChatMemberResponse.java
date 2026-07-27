package com.nguyenquyen.chatservice.chat.dto;

import com.nguyenquyen.chatservice.chat.enums.ChatMemberRole;
import java.time.Instant;

public record ChatMemberResponse(
        String userId,
        ChatMemberRole role,
        Instant joinedAt
) {}
