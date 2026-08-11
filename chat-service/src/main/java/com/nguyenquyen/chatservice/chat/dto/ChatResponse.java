package com.nguyenquyen.chatservice.chat.dto;

import com.nguyenquyen.chatservice.chat.enums.ChatType;
import com.nguyenquyen.chatservice.chat.dto.ChatMemberResponse;
import com.nguyenquyen.chatservice.message.enums.MessageType;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ChatResponse(
        UUID id,
        ChatType type,
        String name,
        String attachmentId,
        String ownerId,
        List<ChatMemberResponse> members,
        String lastMessage,
        Instant lastMessageTime,
        MessageType lastMessageType,
        String lastMessageAttachmentId,
        int unreadCount,
        Instant createdAt,
        Instant updatedAt
) {}
