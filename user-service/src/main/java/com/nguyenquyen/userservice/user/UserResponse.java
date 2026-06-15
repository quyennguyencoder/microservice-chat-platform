package com.nguyenquyen.userservice.user;

public record UserResponse(
     String id,
     String username,
     String email,
     String displayName,
     String imageId,
     String bannerImageId,
     String description
) {
}
