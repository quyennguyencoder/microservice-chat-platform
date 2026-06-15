package com.nguyenquyen.chatservice.message;

import java.time.Instant;
import java.util.UUID;

public record MessageResponse(
        UUID id,
        UUID chatId,
        String senderId,
        String content,
        MessageType type,
        MessageState state,
        String imageId,
        Instant createdAt
) {}