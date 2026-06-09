package com.nguyenquyen.messageservice.controller;

import com.nguyenquyen.messageservice.dto.MessageResponse;
import com.nguyenquyen.messageservice.dto.SendMessageRequest;
import com.nguyenquyen.messageservice.dto.UnreadCountResponse;
import com.nguyenquyen.messageservice.service.MessageService;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.util.UUID;


@RestController
@RequestMapping(value = "/api/messages", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class MessageController {

    private final MessageService messageService;

    // ─── Send Message ──────────────────────────────────────────────────────────

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<MessageResponse> sendMessage(
            @RequestHeader("X-User-Id") String userId,
            @Valid @RequestBody SendMessageRequest request) {

        MessageResponse response = messageService.sendMessage(UUID.fromString(userId), request);
        return ResponseEntity.status(201).body(response);
    }

    // ─── Get Messages in a Chat ────────────────────────────────────────────────


    @GetMapping("/{chatId}")
    public ResponseEntity<Page<MessageResponse>> getMessages(
            @PathVariable UUID chatId,
            @RequestHeader("X-User-Id") String userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Page<MessageResponse> messages = messageService.getMessages(chatId, page, size);
        return ResponseEntity.ok(messages);
    }

    // ─── Mark Chat as Read ─────────────────────────────────────────────────────

    @PutMapping(value = "/{chatId}/read")
    public ResponseEntity<Void> markChatAsRead(
            @PathVariable UUID chatId,
            @RequestHeader("X-User-Id") String userId) {

        messageService.markChatAsRead(chatId, UUID.fromString(userId));
        return ResponseEntity.noContent().build();
    }

    // ─── Get Unread Count ──────────────────────────────────────────────────────

    @GetMapping("/{chatId}/unread")
    public ResponseEntity<UnreadCountResponse> getUnreadCount(
            @PathVariable UUID chatId,
            @RequestHeader("X-User-Id") String userId) {

        UnreadCountResponse response = messageService.getUnreadCount(chatId, UUID.fromString(userId));
        return ResponseEntity.ok(response);
    }

    // ─── Delete Message ────────────────────────────────────────────────────────

    @DeleteMapping("/{messageId}")
    public ResponseEntity<Void> deleteMessage(
            @PathVariable UUID messageId,
            @RequestHeader("X-User-Id") String userId) {

        messageService.deleteMessage(messageId, UUID.fromString(userId));
        return ResponseEntity.noContent().build();
    }
}
