package com.nguyenquyen.conversationservice.service;

import com.nguyenquyen.conversationservice.dto.*;
import com.nguyenquyen.conversationservice.entity.*;
import com.nguyenquyen.conversationservice.event.GroupCreatedEvent;
import com.nguyenquyen.conversationservice.event.GroupMemberEvent;
import com.nguyenquyen.conversationservice.exception.GroupAccessDeniedException;
import com.nguyenquyen.conversationservice.exception.GroupNotFoundException;
import com.nguyenquyen.conversationservice.repository.GroupMemberRepository;
import com.nguyenquyen.conversationservice.repository.GroupRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class GroupService {

    private static final String GROUP_EVENTS_TOPIC = "group-events";

    private final GroupRepository groupRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final ChatService chatService;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    // ─── Create ──────────────────────────────────────────────────────────────

    /**
     * Creates a new group with the creator as OWNER, then immediately creates
     * the GROUP chat in the same service (no Kafka round-trip needed).
     * Finally publishes GroupCreatedEvent to Kafka for notification-service.
     */
    @Transactional
    public GroupResponse createGroup(CreateGroupRequest request, UUID creatorId) {
        Group group = Group.builder()
                .name(request.getName())
                .description(request.getDescription())
                .avatarUrl(request.getAvatarUrl())
                .createdBy(creatorId)
                .build();

        // Creator becomes OWNER
        GroupMember owner = GroupMember.builder()
                .group(group)
                .userId(creatorId)
                .role(GroupRole.OWNER)
                .build();
        group.getMembers().add(owner);

        // Optional initial members (skip creator if accidentally included)
        if (request.getMemberIds() != null) {
            for (UUID memberId : request.getMemberIds()) {
                if (!memberId.equals(creatorId)) {
                    GroupMember member = GroupMember.builder()
                            .group(group)
                            .userId(memberId)
                            .role(GroupRole.MEMBER)
                            .build();
                    group.getMembers().add(member);
                }
            }
        }

        Group saved = groupRepository.save(group);
        log.info("Group created: id={}, name={}, members={}", saved.getId(), saved.getName(), saved.getMembers().size());

        // Create group chat directly — no Kafka round-trip
        List<UUID> allMemberIds = saved.getMembers().stream()
                .map(GroupMember::getUserId)
                .collect(Collectors.toList());

        ChatResponse chatResponse = chatService.createGroupChat(saved.getId(), allMemberIds);

        // Link chatId back to group immediately (synchronous — no null window)
        saved.setChatId(chatResponse.getId());
        groupRepository.save(saved);
        log.info("Linked chatId={} to groupId={}", chatResponse.getId(), saved.getId());

        // Publish event to Kafka for notification-service
        GroupCreatedEvent event = GroupCreatedEvent.builder()
                .eventType("GROUP_CREATED")
                .groupId(saved.getId())
                .groupName(saved.getName())
                .createdBy(creatorId)
                .memberIds(allMemberIds)
                .createdAt(saved.getCreatedAt())
                .build();
        kafkaTemplate.send(GROUP_EVENTS_TOPIC, saved.getId().toString(), event);

        return GroupResponse.from(saved);
    }

    // ─── Read ────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public GroupResponse getGroupById(UUID groupId, UUID requesterId) {
        Group group = findGroupOrThrow(groupId);
        requireMember(groupId, requesterId, "You are not a member of this group");
        return GroupResponse.from(group);
    }

    @Transactional(readOnly = true)
    public Page<GroupResponse> getMyGroups(UUID requesterId, Pageable pageable) {
        return groupRepository.findAllByMemberUserId(requesterId, pageable)
                .map(GroupResponse::from);
    }

    @Transactional(readOnly = true)
    public List<GroupMemberResponse> getMembers(UUID groupId, UUID requesterId) {
        Group group = findGroupOrThrow(groupId);
        requireMember(groupId, requesterId, "You are not a member of this group");
        return group.getMembers().stream()
                .map(GroupMemberResponse::from)
                .collect(Collectors.toList());
    }

    // ─── Update ──────────────────────────────────────────────────────────────

    @Transactional
    public GroupResponse updateGroup(UUID groupId, UpdateGroupRequest request, UUID requesterId) {
        Group group = findGroupOrThrow(groupId);
        requireOwnerOrAdmin(groupId, requesterId, "Only OWNER or ADMIN can update group info");

        if (request.getName() != null && !request.getName().isBlank()) {
            group.setName(request.getName());
        }
        if (request.getDescription() != null) {
            group.setDescription(request.getDescription());
        }
        if (request.getAvatarUrl() != null) {
            group.setAvatarUrl(request.getAvatarUrl());
        }
        if (request.getChatId() != null) {
            group.setChatId(request.getChatId());
        }

        return GroupResponse.from(groupRepository.save(group));
    }

    // ─── Delete ──────────────────────────────────────────────────────────────

    @Transactional
    public void deleteGroup(UUID groupId, UUID requesterId) {
        findGroupOrThrow(groupId);
        requireOwner(groupId, requesterId, "Only the OWNER can delete a group");

        // Delete the associated group chat (if exists)
        chatService.deleteGroupChat(groupId);

        // Publish event so notification-service can clean up its cache
        publishGroupMemberEvent("GROUP_DELETED", groupId, null);

        groupRepository.deleteById(groupId);
        log.info("Group deleted: id={} by userId={}", groupId, requesterId);
    }

    // ─── Members ─────────────────────────────────────────────────────────────

    @Transactional
    public GroupMemberResponse addMember(UUID groupId, AddMemberRequest request, UUID requesterId) {
        findGroupOrThrow(groupId);
        requireOwnerOrAdmin(groupId, requesterId, "Only OWNER or ADMIN can add members");

        if (groupMemberRepository.existsByGroupIdAndUserId(groupId, request.getUserId())) {
            throw new IllegalArgumentException("User is already a member of this group");
        }

        Group group = findGroupOrThrow(groupId);
        GroupMember member = GroupMember.builder()
                .group(group)
                .userId(request.getUserId())
                .role(GroupRole.MEMBER)
                .build();

        GroupMember saved = groupMemberRepository.save(member);
        log.info("Member added to group {}: userId={}", groupId, request.getUserId());

        // Sync to group chat directly
        chatService.addParticipantToGroupChat(groupId, request.getUserId());

        // Notify via Kafka (notification-service)
        publishGroupMemberEvent("MEMBER_ADDED", groupId, request.getUserId());

        return GroupMemberResponse.from(saved);
    }

    @Transactional
    public void removeMember(UUID groupId, UUID targetUserId, UUID requesterId) {
        findGroupOrThrow(groupId);

        GroupMember target = groupMemberRepository
                .findByGroupIdAndUserId(groupId, targetUserId)
                .orElseThrow(() -> new IllegalArgumentException("Target user is not a member of this group"));

        boolean isSelf = requesterId.equals(targetUserId);

        if (isSelf) {
            if (target.getRole() == GroupRole.OWNER) {
                throw new GroupAccessDeniedException("OWNER cannot leave the group. Delete the group instead.");
            }
            groupMemberRepository.deleteByGroupIdAndUserId(groupId, targetUserId);
            chatService.removeParticipantFromGroupChat(groupId, targetUserId);
            publishGroupMemberEvent("MEMBER_REMOVED", groupId, targetUserId);
            log.info("User {} left group {}", requesterId, groupId);
            return;
        }

        requireOwnerOrAdmin(groupId, requesterId, "Only OWNER or ADMIN can remove other members");

        boolean requesterIsAdmin = groupMemberRepository
                .existsByGroupIdAndUserIdAndRole(groupId, requesterId, GroupRole.ADMIN);
        if (requesterIsAdmin && target.getRole() != GroupRole.MEMBER) {
            throw new GroupAccessDeniedException("ADMIN can only remove MEMBER-role users");
        }

        groupMemberRepository.deleteByGroupIdAndUserId(groupId, targetUserId);
        chatService.removeParticipantFromGroupChat(groupId, targetUserId);
        publishGroupMemberEvent("MEMBER_REMOVED", groupId, targetUserId);
        log.info("Member {} removed from group {} by {}", targetUserId, groupId, requesterId);
    }

    @Transactional
    public GroupMemberResponse updateMemberRole(UUID groupId, UUID targetUserId,
                                                UpdateMemberRoleRequest request, UUID requesterId) {
        findGroupOrThrow(groupId);
        requireOwner(groupId, requesterId, "Only the OWNER can change member roles");

        if (requesterId.equals(targetUserId)) {
            throw new GroupAccessDeniedException("OWNER cannot change their own role");
        }

        GroupMember target = groupMemberRepository
                .findByGroupIdAndUserId(groupId, targetUserId)
                .orElseThrow(() -> new IllegalArgumentException("Target user is not a member of this group"));

        if (request.getRole() == GroupRole.OWNER) {
            throw new IllegalArgumentException("Cannot promote to OWNER. Ownership transfer is not supported.");
        }

        target.setRole(request.getRole());
        GroupMember saved = groupMemberRepository.save(target);
        log.info("Role of user {} in group {} changed to {} by {}", targetUserId, groupId, request.getRole(), requesterId);
        return GroupMemberResponse.from(saved);
    }

    // ─── Private helpers ─────────────────────────────────────────────────────

    private Group findGroupOrThrow(UUID groupId) {
        return groupRepository.findById(groupId)
                .orElseThrow(() -> new GroupNotFoundException(groupId));
    }

    private void requireMember(UUID groupId, UUID userId, String message) {
        if (!groupMemberRepository.existsByGroupIdAndUserId(groupId, userId)) {
            throw new GroupAccessDeniedException(message);
        }
    }

    private void requireOwnerOrAdmin(UUID groupId, UUID userId, String message) {
        boolean isOwner = groupMemberRepository.existsByGroupIdAndUserIdAndRole(groupId, userId, GroupRole.OWNER);
        boolean isAdmin = groupMemberRepository.existsByGroupIdAndUserIdAndRole(groupId, userId, GroupRole.ADMIN);
        if (!isOwner && !isAdmin) {
            throw new GroupAccessDeniedException(message);
        }
    }

    private void requireOwner(UUID groupId, UUID userId, String message) {
        if (!groupMemberRepository.existsByGroupIdAndUserIdAndRole(groupId, userId, GroupRole.OWNER)) {
            throw new GroupAccessDeniedException(message);
        }
    }

    private void publishGroupMemberEvent(String eventType, UUID groupId, UUID userId) {
        GroupMemberEvent event = GroupMemberEvent.builder()
                .eventType(eventType)
                .groupId(groupId)
                .userId(userId)
                .build();
        kafkaTemplate.send(GROUP_EVENTS_TOPIC, groupId.toString(), event);
        log.debug("Published {} event for groupId={}, userId={}", eventType, groupId, userId);
    }
}
