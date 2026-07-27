package com.nguyenquyen.chatservice.chat.dto;

import com.nguyenquyen.chatservice.chat.enums.ChatMemberRole;
import jakarta.validation.constraints.NotNull;

public record ChangeRoleRequest(
        @NotNull(message = "Role is required")
        ChatMemberRole role
) {}
