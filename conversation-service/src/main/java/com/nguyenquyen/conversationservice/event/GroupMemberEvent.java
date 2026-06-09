package com.nguyenquyen.conversationservice.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * Published to {@code group-events} topic for member lifecycle changes.
 * eventType: MEMBER_ADDED | MEMBER_REMOVED | GROUP_DELETED
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GroupMemberEvent {

    private String eventType;
    private UUID groupId;
    /** Null for GROUP_DELETED events. */
    private UUID userId;
    private Instant createdAt;
}
