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
public class GroupEvent {

    /** Discriminator: GROUP_CREATED | MEMBER_ADDED | MEMBER_REMOVED | GROUP_DELETED. */
    private String eventType;

    /** The group this event relates to. */
    private UUID groupId;

    /** Human-readable group name (present on GROUP_CREATED). */
    private String groupName;

    /** The user who created the group (present on GROUP_CREATED). */
    private UUID createdBy;

    /** Initial member list (present on GROUP_CREATED). */
    private List<UUID> memberIds;

    /** The affected user's UUID (present on MEMBER_ADDED / MEMBER_REMOVED). */
    private UUID userId;

    /** Event timestamp. */
    private Instant createdAt;
}
