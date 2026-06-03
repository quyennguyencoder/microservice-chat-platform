package com.nguyenquyen.common.constant;

public final class KafkaTopics {
    public static final String ACCOUNT_REGISTERED = "account.registered";
    public static final String USER_CREATED = "user.created";
    public static final String USER_UPDATED = "user.updated";
    public static final String FRIEND_REQUEST_CREATED = "friend.request.created";
    public static final String FRIEND_REQUEST_ACCEPTED = "friend.request.accepted";
    public static final String MESSAGE_CREATED = "message.created";
    public static final String MESSAGE_UPDATED = "message.updated";
    public static final String MESSAGE_DELETED = "message.deleted";
    public static final String NOTIFICATION_CREATED = "notification.created";

    private KafkaTopics() {
    }
}
