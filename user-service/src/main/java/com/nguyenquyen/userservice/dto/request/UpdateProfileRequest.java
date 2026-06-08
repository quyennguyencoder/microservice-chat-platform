package com.nguyenquyen.userservice.dto.request;

import com.nguyenquyen.userservice.enums.OnlineStatus;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;


@Data
public class UpdateProfileRequest {

    @Size(min = 2, max = 100, message = "Display name must be 2–100 characters")
    private String displayName;


    @Size(min = 3, max = 50, message = "Username must be 3–50 characters")
    @Pattern(
        regexp = "^[a-zA-Z0-9][a-zA-Z0-9._-]*$",
        message = "Username can only contain letters, numbers, dots, underscores, and hyphens"
    )
    private String username;

    @Size(max = 500, message = "Bio cannot exceed 500 characters")
    private String bio;

    @Size(max = 1000, message = "Avatar URL is too long")
    private String avatarUrl;

    @Size(max = 20, message = "Phone number too long")
    private String phone;

    private LocalDate dateOfBirth;

    @Size(max = 100, message = "Location cannot exceed 100 characters")
    private String location;

    @Size(max = 255, message = "Website URL cannot exceed 255 characters")
    private String website;

    private OnlineStatus onlineStatus;
}
