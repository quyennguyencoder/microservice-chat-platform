package com.nguyenquyen.chatservice.message.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.nguyenquyen.common.dto.response.ApiResponse;
import com.nguyenquyen.common.dto.response.PageResponse;
import com.nguyenquyen.chatservice.message.service.MessageService;
import com.nguyenquyen.chatservice.message.dto.MessageResponse;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/chats/{chatId}/messages")
@RequiredArgsConstructor
public class MessageController {

    private final MessageService messageService;

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<MessageResponse>>> getMessages(
            @PathVariable UUID chatId,
            @PageableDefault(size = 50, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable
    ) {
        Page<MessageResponse> page = messageService.getChatMessages(chatId, pageable);
        PageResponse<MessageResponse> pageResponse = new PageResponse<>(
                page.getNumber(), page.getTotalPages(), page.getSize(), page.getTotalElements(), page.getContent()
        );
        return ResponseEntity.ok(ApiResponse.success(pageResponse));
    }

    @GetMapping("/all")
    public ResponseEntity<ApiResponse<List<MessageResponse>>> getAllMessages(@PathVariable UUID chatId) {
        return ResponseEntity.ok(ApiResponse.success(messageService.getAllChatMessages(chatId)));
    }

    @PatchMapping("/read")
    public ResponseEntity<ApiResponse<Integer>> markAsRead(@PathVariable UUID chatId) {
        return ResponseEntity.ok(ApiResponse.success(messageService.markAsRead(chatId)));
    }

    @GetMapping("/unread/count")
    public ResponseEntity<ApiResponse<Integer>> getUnreadCount(@PathVariable UUID chatId) {
        return ResponseEntity.ok(ApiResponse.success(messageService.getUnreadCount(chatId)));
    }

    @GetMapping("/{messageId}/page")
    public ResponseEntity<ApiResponse<Integer>> getMessagePage(
            @PathVariable UUID chatId,
            @PathVariable UUID messageId,
            @RequestParam(defaultValue = "50") int size
    ) {
        return ResponseEntity.ok(ApiResponse.success(messageService.getMessagePage(chatId, messageId, size)));
    }
}