package com.nguyenquyen.conversationservice.dto;

import com.nguyenquyen.conversationservice.entity.Group;
import com.nguyenquyen.conversationservice.entity.GroupMember;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GroupResponse {

    private UUID id;
    private String name;
    private String description;
    private String avatarUrl;
    private UUID createdBy;
    private UUID chatId;
    private int memberCount;
    private List<GroupMemberResponse> members;
    private Instant createdAt;
    private Instant updatedAt;

    public static GroupResponse from(Group group) {
        List<GroupMemberResponse> memberResponses = group.getMembers().stream()
                .map(GroupMemberResponse::from)
                .collect(Collectors.toList());

        return GroupResponse.builder()
                .id(group.getId())
                .name(group.getName())
                .description(group.getDescription())
                .avatarUrl(group.getAvatarUrl())
                .createdBy(group.getCreatedBy())
                .chatId(group.getChatId())
                .memberCount(memberResponses.size())
                .members(memberResponses)
                .createdAt(group.getCreatedAt())
                .updatedAt(group.getUpdatedAt())
                .build();
    }
}
