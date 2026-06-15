package com.nguyenquyen.chatservice.chat;

import jakarta.validation.constraints.NotNull;

public record ChangeRoleRequest(
        @NotNull(message = "Role is required")
        ChatMemberRole role
) {}
