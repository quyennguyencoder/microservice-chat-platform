package com.nguyenquyen.chatservice.kafka;

public enum ChatEventType {
    NEW_MESSAGE,
    MESSAGES_READ,
    USER_ONLINE,
    USER_OFFLINE,
    GROUP_CREATED,
    GROUP_UPDATED,
    MEMBER_ADDED,
    MEMBER_REMOVED
}