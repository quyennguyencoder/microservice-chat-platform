package com.nguyenquyen.chatservice.chat;

import com.nguyenquyen.chatservice.message.MessageType;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ChatResponse(
        UUID id,
        ChatType type,
        String name,
        String imageId,
        String ownerId,
        List<ChatMemberResponse> members,
        String lastMessage,
        Instant lastMessageTime,
        MessageType lastMessageType,
        String lastMessageImageId,
        int unreadCount,
        Instant createdAt,
        Instant updatedAt
) {}
