package com.nguyenquyen.conversationservice.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class AddMemberRequest {

    @NotNull(message = "userId is required")
    private UUID userId;
}
