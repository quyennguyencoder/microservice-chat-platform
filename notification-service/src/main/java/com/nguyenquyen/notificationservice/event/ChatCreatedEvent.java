package com.nguyenquyen.notificationservice.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.UUID;


@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ChatCreatedEvent {

    /** UUID of the newly created chat. */
    private UUID chatId;

    /** Chat type: "PRIVATE" or "GROUP". */
    private String type;

    /** Group UUID this chat belongs to — {@code null} for PRIVATE chats. */
    private UUID groupId;

    /** All initial participant UUIDs for this chat. */
    private List<UUID> participantIds;

    /** Timestamp when the chat was created. */
    private Instant createdAt;
}
