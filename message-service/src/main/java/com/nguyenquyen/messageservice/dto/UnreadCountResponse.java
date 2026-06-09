package com.nguyenquyen.messageservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UnreadCountResponse {

    /** UUID of the chat. */
    private UUID chatId;

    /** Number of unread messages for the requesting user. */
    private long unreadCount;
}
