package com.nguyenquyen.conversationservice.event;

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
public class ChatCreatedEvent {

    /** UUID of the newly created chat. */
    private UUID chatId;

    /** Type of the chat (PRIVATE or GROUP). */
    private ChatType type;

    /** Group ID this chat belongs to. Null for PRIVATE chats. */
    private UUID groupId;

    /** UUIDs of all participants in this chat. */
    private List<UUID> participantIds;

    /** Timestamp when the chat was created. */
    private LocalDateTime createdAt;
}
