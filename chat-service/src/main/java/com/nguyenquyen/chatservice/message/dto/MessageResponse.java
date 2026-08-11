package com.nguyenquyen.chatservice.message.dto;

import com.nguyenquyen.chatservice.message.enums.MessageType;
import com.nguyenquyen.chatservice.message.enums.MessageState;

import java.time.Instant;
import java.util.UUID;

public record MessageResponse(
        UUID id,
        UUID chatId,
        String senderId,
        String content,
        MessageType type,
        MessageState state,
        String attachmentId,
        Instant createdAt
) {}