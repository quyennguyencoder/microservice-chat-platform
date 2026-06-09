package com.nguyenquyen.conversationservice.controller;

import com.nguyenquyen.conversationservice.dto.*;
import com.nguyenquyen.conversationservice.service.GroupService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping(value = "/api/groups", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class GroupController {

    private final GroupService groupService;

    // ─── Create Group ────────────────────────────────────────────────────────

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<GroupResponse> createGroup(
            @Valid @RequestBody CreateGroupRequest request,
            @RequestHeader("X-User-Id") UUID userId) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(groupService.createGroup(request, userId));
    }

    // ─── Get My Groups ────────────────────────────────────────────────────────

    @GetMapping("/my")
    public ResponseEntity<Page<GroupResponse>> getMyGroups(
            @RequestHeader("X-User-Id") UUID userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(groupService.getMyGroups(userId, pageable));
    }

    // ─── Get Group By ID ─────────────────────────────────────────────────────

    @GetMapping("/{groupId}")
    public ResponseEntity<GroupResponse> getGroupById(
            @PathVariable UUID groupId,
            @RequestHeader("X-User-Id") UUID userId) {
        return ResponseEntity.ok(groupService.getGroupById(groupId, userId));
    }

    // ─── Update Group ────────────────────────────────────────────────────────

    @PutMapping(value = "/{groupId}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<GroupResponse> updateGroup(
            @PathVariable UUID groupId,
            @Valid @RequestBody UpdateGroupRequest request,
            @RequestHeader("X-User-Id") UUID userId) {
        return ResponseEntity.ok(groupService.updateGroup(groupId, request, userId));
    }

    // ─── Delete Group ────────────────────────────────────────────────────────

    @DeleteMapping("/{groupId}")
    public ResponseEntity<Void> deleteGroup(
            @PathVariable UUID groupId,
            @RequestHeader("X-User-Id") UUID userId) {
        groupService.deleteGroup(groupId, userId);
        return ResponseEntity.noContent().build();
    }

    // ─── List Members ────────────────────────────────────────────────────────

    @GetMapping("/{groupId}/members")
    public ResponseEntity<List<GroupMemberResponse>> getMembers(
            @PathVariable UUID groupId,
            @RequestHeader("X-User-Id") UUID userId) {
        return ResponseEntity.ok(groupService.getMembers(groupId, userId));
    }

    // ─── Add Member ──────────────────────────────────────────────────────────

    @PostMapping(value = "/{groupId}/members", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<GroupMemberResponse> addMember(
            @PathVariable UUID groupId,
            @Valid @RequestBody AddMemberRequest request,
            @RequestHeader("X-User-Id") UUID userId) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(groupService.addMember(groupId, request, userId));
    }

    // ─── Remove Member / Leave ────────────────────────────────────────────────

    @DeleteMapping("/{groupId}/members/{targetUserId}")
    public ResponseEntity<Void> removeMember(
            @PathVariable UUID groupId,
            @PathVariable UUID targetUserId,
            @RequestHeader("X-User-Id") UUID userId) {
        groupService.removeMember(groupId, targetUserId, userId);
        return ResponseEntity.noContent().build();
    }

    // ─── Update Member Role ───────────────────────────────────────────────────

    @PatchMapping(value = "/{groupId}/members/{targetUserId}/role",
                  consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<GroupMemberResponse> updateMemberRole(
            @PathVariable UUID groupId,
            @PathVariable UUID targetUserId,
            @Valid @RequestBody UpdateMemberRoleRequest request,
            @RequestHeader("X-User-Id") UUID userId) {
        return ResponseEntity.ok(groupService.updateMemberRole(groupId, targetUserId, request, userId));
    }
}
