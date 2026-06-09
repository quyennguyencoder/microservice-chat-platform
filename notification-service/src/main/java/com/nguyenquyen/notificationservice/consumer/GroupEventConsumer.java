package com.nguyenquyen.notificationservice.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nguyenquyen.notificationservice.event.GroupEvent;
import com.nguyenquyen.notificationservice.entity.NotificationType;
import com.nguyenquyen.notificationservice.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;


@Component
@RequiredArgsConstructor
@Slf4j
public class GroupEventConsumer {

    private final NotificationService notificationService;
    private final ObjectMapper        objectMapper;

    @KafkaListener(
        topics          = "group-events",
        groupId         = "notification-service",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleGroupEvent(String message) {
        try {
            GroupEvent event = objectMapper.readValue(message, GroupEvent.class);
            log.debug("[GroupEventConsumer] Received GroupEvent: type={} groupId={}",
                    event.getEventType(), event.getGroupId());

            switch (event.getEventType()) {
                case "GROUP_CREATED" -> handleGroupCreated(event);
                case "MEMBER_ADDED"  -> handleMemberAdded(event);
                case "MEMBER_REMOVED"-> handleMemberRemoved(event);
                case "GROUP_DELETED" -> handleGroupDeleted(event);
                default -> log.warn("[GroupEventConsumer] Unknown eventType: {}", event.getEventType());
            }

        } catch (Exception e) {
            log.error("[GroupEventConsumer] Failed to process group-event message: {}", e.getMessage(), e);
        }
    }

    // ─── Private handlers ────────────────────────────────────────

    private void handleGroupCreated(GroupEvent event) {
        List<UUID> memberIds = event.getMemberIds();
        if (memberIds == null || memberIds.isEmpty()) return;

        String groupName = event.getGroupName() != null ? event.getGroupName() : "a new group";

        for (UUID memberId : memberIds) {
            // Skip the creator — they created it, they don't need a "you were added" notification
            if (memberId.equals(event.getCreatedBy())) continue;

            notificationService.saveNotification(
                    memberId,
                    NotificationType.GROUP_CREATED,
                    "You were added to \"" + groupName + "\"",
                    "You have been added to a new group.",
                    event.getGroupId(),
                    "GROUP"
            );
        }
        log.debug("[GroupEventConsumer] Sent GROUP_CREATED notifications for group {}", event.getGroupId());
    }

    private void handleMemberAdded(GroupEvent event) {
        UUID userId  = event.getUserId();
        UUID groupId = event.getGroupId();
        if (userId == null || groupId == null) return;

        // Create notification for the added user
        notificationService.saveNotification(
                userId,
                NotificationType.MEMBER_ADDED,
                "You were added to a group",
                "An admin has added you to a group.",
                groupId,
                "GROUP"
        );

        // Update participant cache: look up chatId via groupId → chatId mapping
        Optional<UUID> chatIdOpt = notificationService.getChatIdByGroupId(groupId);
        if (chatIdOpt.isPresent()) {
            notificationService.addParticipantIfAbsent(chatIdOpt.get(), userId);
            log.debug("[GroupEventConsumer] Added user {} to chat participant cache for group {}", userId, groupId);
        } else {
            log.warn("[GroupEventConsumer] ChatId not yet cached for group {} — participant cache not updated for user {}",
                    groupId, userId);
        }
    }

    private void handleMemberRemoved(GroupEvent event) {
        UUID userId  = event.getUserId();
        UUID groupId = event.getGroupId();
        if (userId == null || groupId == null) return;

        Optional<UUID> chatIdOpt = notificationService.getChatIdByGroupId(groupId);
        chatIdOpt.ifPresent(chatId ->
                notificationService.removeParticipant(chatId, userId));

        log.debug("[GroupEventConsumer] Removed user {} from participant cache for group {}", userId, groupId);
    }

    private void handleGroupDeleted(GroupEvent event) {
        UUID groupId = event.getGroupId();
        if (groupId == null) return;

        Optional<UUID> chatIdOpt = notificationService.getChatIdByGroupId(groupId);
        chatIdOpt.ifPresent(chatId -> {
            notificationService.removeAllParticipantsForChat(chatId);
            log.debug("[GroupEventConsumer] Cleared participant cache for deleted group {}", groupId);
        });
    }
}
