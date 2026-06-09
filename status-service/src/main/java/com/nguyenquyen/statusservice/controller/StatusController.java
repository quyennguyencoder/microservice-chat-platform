package com.nguyenquyen.statusservice.controller;


import com.nguyenquyen.statusservice.dto.response.UserStatusResponse;
import com.nguyenquyen.statusservice.entity.Status;
import com.nguyenquyen.statusservice.service.StatusService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;


@RestController
@RequestMapping(value = "/api/status", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
@Slf4j
public class StatusController {

    private final StatusService statusService;

    // ─── PUT /api/status/me ──────────────────────────────────────

    @PutMapping(value = "/me")
    public ResponseEntity<Void> updateMyStatus(
            @RequestHeader("X-User-Id") UUID userId,
            @RequestParam Status status) {

        log.debug("[StatusController] PUT /api/status/me userId={} status={}", userId, status);
        statusService.updateMyStatus(userId, status);
        return ResponseEntity.noContent().build();
    }

    // ─── POST /api/status/heartbeat ──────────────────────────────

    @PostMapping("/heartbeat")
    public ResponseEntity<Void> heartbeat(
            @RequestHeader("X-User-Id") UUID userId) {

        log.debug("[StatusController] POST /api/status/heartbeat userId={}", userId);
        statusService.heartbeat(userId);
        return ResponseEntity.noContent().build();
    }

    // ─── GET /api/status/{userId} ────────────────────────────────

    @GetMapping("/{userId}")
    public ResponseEntity<UserStatusResponse> getStatus(
            @PathVariable UUID userId) {

        return ResponseEntity.ok(statusService.getStatus(userId));
    }

    // ─── GET /api/status/batch ───────────────────────────────────


    @GetMapping("/batch")
    public ResponseEntity<List<UserStatusResponse>> getBatchStatus(
            @RequestParam List<UUID> userIds) {

        log.debug("[StatusController] GET /api/status/batch count={}", userIds.size());
        return ResponseEntity.ok(statusService.getBatchStatus(userIds));
    }
}
