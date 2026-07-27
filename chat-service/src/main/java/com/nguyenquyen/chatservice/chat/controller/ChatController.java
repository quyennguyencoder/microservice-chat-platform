package com.nguyenquyen.chatservice.chat.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.nguyenquyen.common.dto.response.ApiResponse;
import com.nguyenquyen.chatservice.chat.service.ChatService;
import com.nguyenquyen.chatservice.chat.dto.AddMembersRequest;
import com.nguyenquyen.chatservice.chat.dto.ChangeRoleRequest;
import com.nguyenquyen.chatservice.chat.dto.ChatMemberResponse;
import com.nguyenquyen.chatservice.chat.dto.ChatResponse;
import com.nguyenquyen.chatservice.chat.dto.CreateGroupRequest;
import com.nguyenquyen.chatservice.chat.dto.UpdateGroupRequest;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/chats")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    @PostMapping("/with/{targetUserId}")
    public ResponseEntity<ApiResponse<ChatResponse>> getOrCreatePrivateChat(@PathVariable String targetUserId) {
        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponse.success(chatService.getOrCreatePrivateChat(targetUserId)));
    }

    @PostMapping("/group")
    public ResponseEntity<ApiResponse<ChatResponse>> createGroupChat(@Valid @RequestBody CreateGroupRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(chatService.createGroupChat(request)));
    }

    @PatchMapping("/{chatId}/group")
    public ResponseEntity<ApiResponse<ChatResponse>> updateGroupChat(
            @PathVariable UUID chatId,
            @Valid @RequestBody UpdateGroupRequest request) {
        return ResponseEntity.ok(ApiResponse.success(chatService.updateGroupChat(chatId, request)));
    }

    @PostMapping("/{chatId}/members")
    public ResponseEntity<ApiResponse<ChatResponse>> addMembers(
            @PathVariable UUID chatId,
            @Valid @RequestBody AddMembersRequest request) {
        return ResponseEntity.ok(ApiResponse.success(chatService.addMembers(chatId, request)));
    }

    @DeleteMapping("/{chatId}/members/{memberId}")
    public ResponseEntity<ApiResponse<Void>> removeMember(
            @PathVariable UUID chatId,
            @PathVariable String memberId) {
        chatService.removeMember(chatId, memberId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @PostMapping("/{chatId}/leave")
    public ResponseEntity<ApiResponse<Void>> leaveChat(@PathVariable UUID chatId) {
        chatService.leaveChat(chatId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @PatchMapping("/{chatId}/members/{userId}/role")
    public ResponseEntity<ApiResponse<ChatResponse>> changeMemberRole(
            @PathVariable UUID chatId,
            @PathVariable String userId,
            @Valid @RequestBody ChangeRoleRequest request) {
        return ResponseEntity.ok(ApiResponse.success(chatService.changeMemberRole(chatId, userId, request)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<ChatResponse>>> getMyChats() {
        return ResponseEntity.ok(ApiResponse.success(chatService.getMyChats()));
    }

    @GetMapping("/{chatId}")
    public ResponseEntity<ApiResponse<ChatResponse>> getChatById(@PathVariable UUID chatId) {
        return ResponseEntity.ok(ApiResponse.success(chatService.getChatById(chatId)));
    }

    @DeleteMapping("/{chatId}")
    public ResponseEntity<ApiResponse<Void>> deleteChat(@PathVariable UUID chatId) {
        chatService.deleteChat(chatId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}