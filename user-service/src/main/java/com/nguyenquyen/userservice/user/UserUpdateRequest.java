package com.nguyenquyen.userservice.user;

import jakarta.validation.constraints.Size;

public record UserUpdateRequest(

        @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
        String username,

        @Size(max = 100, message = "Display name must not exceed 100 characters")
        String displayName,

        @Size(max = 500, message = "Description must not exceed 500 characters")
        String description,

        @Size(max = 100)
        String imageId,

        @Size(max = 100)
        String bannerImageId
) {
}