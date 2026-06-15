package com.nguyenquyen.chatservice.presence;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Set;

@RestController
@RequestMapping("/api/v1/presence")
@RequiredArgsConstructor
public class PresenceController {

    private final PresenceService presenceService;

    @GetMapping
    public ResponseEntity<UserPresenceResponse> getPresenceData(
            @RequestParam Set<String> userIds) {
        return ResponseEntity.ok(presenceService.getPresenceData(userIds));
    }

    @GetMapping("/{userId}")
    public ResponseEntity<UserPresenceResponse.UserPresence> getUserPresence(
            @PathVariable String userId) {
        return ResponseEntity.ok(presenceService.getUserPresence(userId));
    }

    @GetMapping("/{userId}/online")
    public ResponseEntity<Boolean> isUserOnline(@PathVariable String userId) {
        return ResponseEntity.ok(presenceService.isUserOnline(userId));
    }
}