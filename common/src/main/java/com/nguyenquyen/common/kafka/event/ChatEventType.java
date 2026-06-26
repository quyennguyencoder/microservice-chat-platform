package com.nguyenquyen.common.kafka.event;

public enum ChatEventType {
    NEW_MESSAGE,
    MESSAGES_READ,
    USER_ONLINE,
    USER_OFFLINE,
    GROUP_CREATED,
    GROUP_UPDATED,
    MEMBER_ADDED,
    MEMBER_REMOVED,
    MESSAGE_UPDATED,
    MESSAGE_DELETED
}
