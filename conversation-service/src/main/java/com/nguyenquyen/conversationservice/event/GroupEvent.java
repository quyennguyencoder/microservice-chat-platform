package com.nguyenquyen.conversationservice.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Kafka Event — consumed from the {@code group-events} topic.
 *
 * <p>Superset DTO covering all event types published by the Group Service.
 * The {@code eventType} discriminator determines which fields are populated:</p>
 * <ul>
 *   <li>{@code GROUP_CREATED}  — groupId, groupName, createdBy, memberIds, createdAt</li>
 *   <li>{@code MEMBER_ADDED}   — groupId, userId</li>
 *   <li>{@code MEMBER_REMOVED} — groupId, userId</li>
 *   <li>{@code GROUP_DELETED}  — groupId</li>
 * </ul>
 *
 * <p>Jackson silently ignores unknown fields, so both {@code GroupCreatedEvent} and
 * {@code GroupMemberEvent} JSON messages deserialize cleanly into this class.</p>
 *
 * @author  Aniket Kamlesh
 * @version 1.0.0
 * @since   1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GroupEvent {

    private String eventType;
    private UUID groupId;
    private String groupName;
    private UUID createdBy;

    /** All initial members — only for GROUP_CREATED. */
    private List<UUID> memberIds;

    /** The affected user — only for MEMBER_ADDED / MEMBER_REMOVED. */
    private UUID userId;

    private LocalDateTime createdAt;
}
