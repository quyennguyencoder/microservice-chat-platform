package com.nguyenquyen.conversationservice.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

/**
 * Request body for creating a GROUP type chat.
 *
 * <p>Called by the Group Service (service-to-service) immediately after a group is
 * created, so that the group chat is available for messaging right away.</p>
 *
 * @author  Aniket Kamlesh
 * @version 1.0.0
 * @since   1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateGroupChatRequest {

    /**
     * UUIDs of all initial members to add as participants.
     * Must include at least the group creator.
     */
    @NotNull(message = "Member IDs list is required")
    @Size(min = 1, message = "At least one member is required")
    private List<UUID> memberIds;
}
