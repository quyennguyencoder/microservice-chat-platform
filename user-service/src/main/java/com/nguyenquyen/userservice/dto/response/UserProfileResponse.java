package com.nguyenquyen.userservice.dto.response;


import com.nguyenquyen.userservice.entity.UserProfile;
import com.nguyenquyen.userservice.enums.OnlineStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.Instant;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileResponse {

    private String id;
    private String displayName;
    private String username;
    private String email;
    private String bio;
    private String avatarUrl;
    private String phone;
    private LocalDate dateOfBirth;
    private String location;
    private String website;
    private OnlineStatus onlineStatus;
    private Instant lastSeen;
    private Instant createdAt;
    private Instant updatedAt;


    public static UserProfileResponse from(UserProfile profile) {
        return UserProfileResponse.builder()
                .id(profile.getId().toString())
                .displayName(profile.getDisplayName())
                .username(profile.getUsername())
                .email(profile.getEmail())
                .bio(profile.getBio())
                .avatarUrl(profile.getAvatarUrl())
                .phone(profile.getPhone())
                .dateOfBirth(profile.getDateOfBirth())
                .location(profile.getLocation())
                .website(profile.getWebsite())
                .onlineStatus(profile.getOnlineStatus())
                .lastSeen(profile.getLastSeen())
                .createdAt(profile.getCreatedAt())
                .updatedAt(profile.getUpdatedAt())
                .build();
    }
}
