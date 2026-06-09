package com.nguyenquyen.conversationservice.dto;

import com.nguyenquyen.conversationservice.entity.GroupMember;
import com.nguyenquyen.conversationservice.entity.GroupRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GroupMemberResponse {

    private UUID id;
    private UUID userId;
    private GroupRole role;
    private Instant joinedAt;

    public static GroupMemberResponse from(GroupMember member) {
        return GroupMemberResponse.builder()
                .id(member.getId())
                .userId(member.getUserId())
                .role(member.getRole())
                .joinedAt(member.getJoinedAt())
                .build();
    }
}
