package com.nguyenquyen.chatservice.chat;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CreateGroupRequest(
        @NotBlank(message = "Group name is required")
        @Size(max = 100, message = "Group name must not exceed 100 characters")
        String name,

        String imageId,

        @NotEmpty(message = "At least one member is required")
        List<String> memberIds
) {}
