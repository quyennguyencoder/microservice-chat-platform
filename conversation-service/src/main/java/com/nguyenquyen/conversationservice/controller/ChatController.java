package com.nguyenquyen.conversationservice.controller;

import com.nguyenquyen.conversationservice.dto.ChatResponse;
import com.nguyenquyen.conversationservice.dto.CreateGroupChatRequest;
import com.nguyenquyen.conversationservice.dto.CreatePrivateChatRequest;
import com.nguyenquyen.conversationservice.service.ChatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping(value = "/api/chats", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    // ─── Create or Get Private Chat ──────────────────────────────────────────

    @PostMapping(value = "/private", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ChatResponse> createOrGetPrivateChat(
            @RequestHeader("X-User-Id") String userId,
            @Valid @RequestBody CreatePrivateChatRequest request) {
        ChatResponse response = chatService.createOrGetPrivateChat(UUID.fromString(userId), request);
        return ResponseEntity.ok(response);
    }

    // ─── Create Group Chat (internal) ────────────────────────────────────────

    @PostMapping(value = "/group", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ChatResponse> createGroupChat(
            @RequestHeader("X-User-Id") String userId,
            @Valid @RequestBody CreateGroupChatRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(chatService.createGroupChat(request.getMemberIds()));
    }

    // ─── Get My Chats ────────────────────────────────────────────────────────

    @GetMapping
    public ResponseEntity<Page<ChatResponse>> getMyChats(
            @RequestHeader("X-User-Id") String userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<ChatResponse> chats = chatService.getMyChats(UUID.fromString(userId), page, size);
        return ResponseEntity.ok(chats);
    }

    // ─── Get Chat By ID ──────────────────────────────────────────────────────

    @GetMapping("/{chatId}")
    public ResponseEntity<ChatResponse> getChatById(
            @PathVariable UUID chatId,
            @RequestHeader("X-User-Id") String userId) {
        ChatResponse response = chatService.getChatById(chatId, UUID.fromString(userId));
        return ResponseEntity.ok(response);
    }

    // ─── Leave Chat ──────────────────────────────────────────────────────────

    @DeleteMapping("/{chatId}/leave")
    public ResponseEntity<Void> leaveChat(
            @PathVariable UUID chatId,
            @RequestHeader("X-User-Id") String userId) {
        chatService.leaveChat(chatId, UUID.fromString(userId));
        return ResponseEntity.noContent().build();
    }
}
