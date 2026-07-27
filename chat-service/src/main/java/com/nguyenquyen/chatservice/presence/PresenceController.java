package com.nguyenquyen.chatservice.presence;

import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.nguyenquyen.common.dto.response.ApiResponse;

import java.util.Set;

@RestController
@RequestMapping("/api/v1/presence")
@RequiredArgsConstructor
public class PresenceController {

    private final PresenceService presenceService;

    @GetMapping
    public ResponseEntity<ApiResponse<UserPresenceResponse>> getPresenceData(
            @RequestParam Set<String> userIds) {
        return ResponseEntity.ok(ApiResponse.success(presenceService.getPresenceData(userIds)));
    }

    @GetMapping("/{userId}")
    public ResponseEntity<ApiResponse<UserPresenceResponse.UserPresence>> getUserPresence(
            @PathVariable String userId) {
        return ResponseEntity.ok(ApiResponse.success(presenceService.getUserPresence(userId)));
    }

    @GetMapping("/{userId}/online")
    public ResponseEntity<ApiResponse<Boolean>> isUserOnline(@PathVariable String userId) {
        return ResponseEntity.ok(ApiResponse.success(presenceService.isUserOnline(userId)));
    }
}