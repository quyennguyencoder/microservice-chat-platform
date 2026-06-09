package com.nguyenquyen.notificationservice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UnreadCountResponse {

    /** Total number of unread notifications for the requesting user. */
    private long count;
}
