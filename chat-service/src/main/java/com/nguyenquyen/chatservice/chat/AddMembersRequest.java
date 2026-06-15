package com.nguyenquyen.chatservice.chat;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record AddMembersRequest(
        @NotEmpty(message = "At least one member ID is required")
        List<String> memberIds
) {}
