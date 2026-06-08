package com.nguyenquyen.userservice.controller;

import com.nguyenquyen.userservice.dto.request.UpdateProfileRequest;
import com.nguyenquyen.userservice.dto.response.UserProfileResponse;
import com.nguyenquyen.userservice.enums.OnlineStatus;
import com.nguyenquyen.userservice.service.UserProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;


@RestController
@RequestMapping(value = "/api/users", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
@Slf4j
public class UserProfileController {

    private final UserProfileService profileService;

    @GetMapping("/me")
    public ResponseEntity<UserProfileResponse> getMyProfile(
            @RequestHeader("X-User-Id") String userId) {

        log.debug("GET /api/users/me — userId={}", userId);
        return ResponseEntity.ok(profileService.getProfileById(userId));
    }


    @PutMapping(value = "/me", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<UserProfileResponse> updateMyProfile(
            @RequestHeader("X-User-Id") String userId,
            @Valid @RequestBody UpdateProfileRequest request) {

        log.debug("PUT /api/users/me — userId={}", userId);
        return ResponseEntity.ok(profileService.updateProfile(userId, request));
    }

    @PutMapping("/me/status")
    public ResponseEntity<Map<String, String>> updateStatus(
            @RequestHeader("X-User-Id") String userId,
            @RequestParam OnlineStatus status) {

        profileService.updateOnlineStatus(userId, status);
        return ResponseEntity.ok(Map.of(
                "message", "Status updated to " + status,
                "userId",  userId,
                "status",  status.name()
        ));
    }


    @GetMapping("/{userId}")
    public ResponseEntity<UserProfileResponse> getProfileById(
            @RequestHeader("X-User-Id") String requestingUserId,
            @PathVariable String userId) {

        log.debug("GET /api/users/{} — requestedBy={}", userId, requestingUserId);
        return ResponseEntity.ok(profileService.getProfileById(userId));
    }


    @GetMapping("/username/{username}")
    public ResponseEntity<UserProfileResponse> getProfileByUsername(
            @RequestHeader("X-User-Id") String requestingUserId,
            @PathVariable String username) {

        return ResponseEntity.ok(profileService.getProfileByUsername(username));
    }

    @GetMapping("/search")
    public ResponseEntity<Page<UserProfileResponse>> searchUsers(
            @RequestHeader("X-User-Id") String requestingUserId,
            @RequestParam String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        if (q == null || q.trim().length() < 2) {
            return ResponseEntity.badRequest().build();
        }

        log.debug("GET /api/users/search?q={} — requestedBy={}", q, requestingUserId);
        return ResponseEntity.ok(profileService.searchUsers(q, page, size));
    }
}
