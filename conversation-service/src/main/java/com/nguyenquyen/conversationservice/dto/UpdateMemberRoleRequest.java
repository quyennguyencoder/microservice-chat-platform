package com.nguyenquyen.conversationservice.dto;

import com.nguyenquyen.conversationservice.entity.GroupRole;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateMemberRoleRequest {

    @NotNull(message = "role is required")
    private GroupRole role;
}
