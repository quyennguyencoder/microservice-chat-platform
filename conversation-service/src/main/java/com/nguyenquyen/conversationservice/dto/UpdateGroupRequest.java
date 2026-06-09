package com.nguyenquyen.conversationservice.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.UUID;

@Data
public class UpdateGroupRequest {

    @Size(max = 100, message = "Group name must not exceed 100 characters")
    private String name;

    @Size(max = 500, message = "Description must not exceed 500 characters")
    private String description;

    private String avatarUrl;

    /** Internal-use only: links the chatId back to the group. */
    private UUID chatId;
}
