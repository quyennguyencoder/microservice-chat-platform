package com.nguyenquyen.messageservice.entity;


public enum MessageType {

    /** Plain text message. */
    TEXT,

    /** Image attachment — content holds the media URL (future Media Service integration). */
    IMAGE,

    /** File attachment — content holds the file URL (future Media Service integration). */
    FILE
}
