package com.nguyenquyen.chatservice.chat;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/chats")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    @PostMapping("/with/{targetUserId}")
    public ResponseEntity<ChatResponse> getOrCreatePrivateChat(@PathVariable String targetUserId) {
        return ResponseEntity.status(HttpStatus.OK)
                .body(chatService.getOrCreatePrivateChat(targetUserId));
    }

    @PostMapping("/group")
    public ResponseEntity<ChatResponse> createGroupChat(@Valid @RequestBody CreateGroupRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(chatService.createGroupChat(request));
    }

    @PatchMapping("/{chatId}/group")
    public ResponseEntity<ChatResponse> updateGroupChat(
            @PathVariable UUID chatId,
            @Valid @RequestBody UpdateGroupRequest request) {
        return ResponseEntity.ok(chatService.updateGroupChat(chatId, request));
    }

    @PostMapping("/{chatId}/members")
    public ResponseEntity<ChatResponse> addMembers(
            @PathVariable UUID chatId,
            @Valid @RequestBody AddMembersRequest request) {
        return ResponseEntity.ok(chatService.addMembers(chatId, request));
    }

    @DeleteMapping("/{chatId}/members/{memberId}")
    public ResponseEntity<Void> removeMember(
            @PathVariable UUID chatId,
            @PathVariable String memberId) {
        chatService.removeMember(chatId, memberId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{chatId}/leave")
    public ResponseEntity<Void> leaveChat(@PathVariable UUID chatId) {
        chatService.leaveChat(chatId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{chatId}/members/{userId}/role")
    public ResponseEntity<ChatResponse> changeMemberRole(
            @PathVariable UUID chatId,
            @PathVariable String userId,
            @Valid @RequestBody ChangeRoleRequest request) {
        return ResponseEntity.ok(chatService.changeMemberRole(chatId, userId, request));
    }

    @GetMapping
    public ResponseEntity<List<ChatResponse>> getMyChats() {
        return ResponseEntity.ok(chatService.getMyChats());
    }

    @GetMapping("/{chatId}")
    public ResponseEntity<ChatResponse> getChatById(@PathVariable UUID chatId) {
        return ResponseEntity.ok(chatService.getChatById(chatId));
    }

    @DeleteMapping("/{chatId}")
    public ResponseEntity<Void> deleteChat(@PathVariable UUID chatId) {
        chatService.deleteChat(chatId);
        return ResponseEntity.noContent().build();
    }
}