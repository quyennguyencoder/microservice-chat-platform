package com.nguyenquyen.notificationservice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;


@Entity
@Table(name = "group_chat_mapping")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GroupChatMapping {

    /** The group UUID — used as the primary key since one group has exactly one chat. */
    @Id
    @Column(name = "group_id")
    private UUID groupId;

    /** The chat UUID linked to this group. */
    @Column(name = "chat_id", nullable = false, unique = true)
    private UUID chatId;
}
