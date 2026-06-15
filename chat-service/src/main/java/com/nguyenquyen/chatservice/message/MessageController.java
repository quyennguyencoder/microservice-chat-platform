package com.nguyenquyen.chatservice.message;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/chats/{chatId}/messages")
@RequiredArgsConstructor
public class MessageController {

    private final MessageService messageService;

    @GetMapping
    public ResponseEntity<Page<MessageResponse>> getMessages(
            @PathVariable UUID chatId,
            @PageableDefault(size = 50, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable
    ) {
        return ResponseEntity.ok(messageService.getChatMessages(chatId, pageable));
    }

    @GetMapping("/all")
    public ResponseEntity<List<MessageResponse>> getAllMessages(@PathVariable UUID chatId) {
        return ResponseEntity.ok(messageService.getAllChatMessages(chatId));
    }

    @PatchMapping("/read")
    public ResponseEntity<Integer> markAsRead(@PathVariable UUID chatId) {
        return ResponseEntity.ok(messageService.markAsRead(chatId));
    }

    @GetMapping("/unread/count")
    public ResponseEntity<Integer> getUnreadCount(@PathVariable UUID chatId) {
        return ResponseEntity.ok(messageService.getUnreadCount(chatId));
    }

    @GetMapping("/{messageId}/page")
    public ResponseEntity<Integer> getMessagePage(
            @PathVariable UUID chatId,
            @PathVariable UUID messageId,
            @RequestParam(defaultValue = "50") int size
    ) {
        return ResponseEntity.ok(messageService.getMessagePage(chatId, messageId, size));
    }
}