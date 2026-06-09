package com.nguyenquyen.conversationservice.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Published to {@code group-events} topic when a group is created. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GroupCreatedEvent {

    private String eventType;   // "GROUP_CREATED"
    private UUID groupId;
    private String groupName;
    private UUID createdBy;
    private List<UUID> memberIds;
    private Instant createdAt;
}
