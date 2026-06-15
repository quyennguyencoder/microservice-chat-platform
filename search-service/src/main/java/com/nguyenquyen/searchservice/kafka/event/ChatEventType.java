package com.nguyenquyen.searchservice.kafka.event;

public enum ChatEventType {
    NEW_MESSAGE,
    MESSAGE_UPDATED,
    MESSAGE_DELETED,
    MESSAGES_READ,
    USER_ONLINE,
    USER_OFFLINE,
    GROUP_CREATED,
    MEMBER_ADDED,
    MEMBER_REMOVED
}
