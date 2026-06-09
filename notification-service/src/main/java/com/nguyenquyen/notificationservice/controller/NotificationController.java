package com.nguyenquyen.notificationservice.controller;

import com.nguyenquyen.notificationservice.dto.response.NotificationResponse;
import com.nguyenquyen.notificationservice.dto.response.UnreadCountResponse;
import com.nguyenquyen.notificationservice.service.NotificationService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping(value = "/api/notifications", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
@Slf4j
public class NotificationController {

    private final NotificationService notificationService;

    // ─── GET /api/notifications ──────────────────────────────────

    @GetMapping
    public ResponseEntity<Page<NotificationResponse>> getMyNotifications(
            @RequestHeader("X-User-Id") UUID userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        log.debug("[NotificationController] GET /api/notifications user={} page={} size={}", userId, page, size);
        return ResponseEntity.ok(notificationService.getMyNotifications(userId, page, size));
    }

    // ─── GET /api/notifications/unread-count ────────────────────

    @GetMapping("/unread-count")
    public ResponseEntity<UnreadCountResponse> getUnreadCount(
            @RequestHeader("X-User-Id") UUID userId) {

        return ResponseEntity.ok(notificationService.getUnreadCount(userId));
    }

    // ─── PUT /api/notifications/{id}/read ───────────────────────

    @PutMapping("/{id}/read")
    public ResponseEntity<Void> markAsRead(
            @PathVariable("id") UUID notificationId,
            @RequestHeader("X-User-Id") UUID userId) {

        notificationService.markAsRead(notificationId, userId);
        return ResponseEntity.noContent().build();
    }

    // ─── PUT /api/notifications/read-all ────────────────────────

    @PutMapping("/read-all")
    public ResponseEntity<Void> markAllAsRead(
            @RequestHeader("X-User-Id") UUID userId) {

        notificationService.markAllAsRead(userId);
        return ResponseEntity.noContent().build();
    }

    // ─── DELETE /api/notifications/{id} ─────────────────────────

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteNotification(
            @PathVariable("id") UUID notificationId,
            @RequestHeader("X-User-Id") UUID userId) {

        notificationService.deleteNotification(notificationId, userId);
        return ResponseEntity.noContent().build();
    }

    // ─── Exception Handlers ──────────────────────────────────────

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleNotFound(EntityNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, String>> handleForbidden(AccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of("error", ex.getMessage()));
    }
}
