package com.nguyenquyen.chatservice.chat;

import jakarta.validation.constraints.Size;

public record UpdateGroupRequest(
        @Size(max = 100, message = "Group name must not exceed 100 characters")
        String name,

        String imageId
) {}
