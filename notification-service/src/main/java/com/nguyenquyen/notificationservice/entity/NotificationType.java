package com.nguyenquyen.notificationservice.entity;


public enum NotificationType {

    /**
     * A new message was sent in a chat the user participates in.
     * referenceId → chatId
     */
    NEW_MESSAGE,

    /**
     * The user was included as an initial member when a new group was created.
     * referenceId → groupId
     */
    GROUP_CREATED,

    /**
     * The user was added to an existing group by an admin or owner.
     * referenceId → groupId
     */
    MEMBER_ADDED
}
