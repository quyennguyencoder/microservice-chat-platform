package com.nguyenquyen.messageservice.entity;


public enum MessageStatus {

    /** Message persisted; not yet confirmed delivered to recipient. */
    SENT,

    /** Message delivered to recipient's client (confirmed via WebSocket ACK). */
    DELIVERED,

    /** Recipient has viewed/read the message. */
    READ
}
