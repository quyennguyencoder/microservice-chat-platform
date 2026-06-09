package com.nguyenquyen.conversationservice.dto;

import com.nguyenquyen.conversationservice.entity.ChatType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatResponse {

    /** Unique identifier of this chat. */
    private UUID id;

    /** Type of chat: PRIVATE (1-on-1) or GROUP. */
    private ChatType type;

    /** Group ID this chat belongs to. Null for PRIVATE chats. */
    private UUID groupId;

    /** All participants in this chat. */
    private List<ParticipantResponse> participants;

    /** Timestamp when the chat was created. */
    private LocalDateTime createdAt;

    /** Timestamp when the chat was last updated. */
    private LocalDateTime updatedAt;
}
