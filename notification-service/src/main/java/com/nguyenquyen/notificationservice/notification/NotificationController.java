package com.nguyenquyen.notificationservice.notification;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.nguyenquyen.common.dto.response.ApiResponse;
import com.nguyenquyen.common.dto.response.PageResponse;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<NotificationResponse>>> getNotifications(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable
    ) {
        Page<NotificationResponse> page = notificationService.getMyNotifications(pageable);
        PageResponse<NotificationResponse> pageResponse = new PageResponse<>(
                page.getNumber(), page.getTotalPages(), page.getSize(), page.getTotalElements(), page.getContent()
        );
        return ResponseEntity.ok(ApiResponse.success(pageResponse));
    }

    @GetMapping("/unread")
    public ResponseEntity<ApiResponse<PageResponse<NotificationResponse>>> getUnreadNotifications(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable
    ) {
        Page<NotificationResponse> page = notificationService.getMyUnreadNotifications(pageable);
        PageResponse<NotificationResponse> pageResponse = new PageResponse<>(
                page.getNumber(), page.getTotalPages(), page.getSize(), page.getTotalElements(), page.getContent()
        );
        return ResponseEntity.ok(ApiResponse.success(pageResponse));
    }

    @GetMapping("/unread/count")
    public ResponseEntity<ApiResponse<Map<String, Long>>> getUnreadCount() {
        return ResponseEntity.ok(ApiResponse.success(Map.of("count", notificationService.getUnreadCount())));
    }

    @PatchMapping("/read-all")
    public ResponseEntity<ApiResponse<Map<String, Integer>>> markAllAsRead() {
        return ResponseEntity.ok(ApiResponse.success(Map.of("updated", notificationService.markAllAsRead())));
    }

    @PatchMapping("/read")
    public ResponseEntity<ApiResponse<Map<String, Integer>>> markAsRead(@RequestBody List<UUID> ids) {
        return ResponseEntity.ok(ApiResponse.success(Map.of("updated", notificationService.markAsRead(ids))));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        notificationService.delete(id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}