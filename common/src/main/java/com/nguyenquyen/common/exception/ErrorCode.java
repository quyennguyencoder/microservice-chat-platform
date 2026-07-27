package com.nguyenquyen.common.exception;

import org.springframework.http.HttpStatus;

public enum ErrorCode {
    UNCATEGORIZED_EXCEPTION(9999, "Uncategorized error", HttpStatus.INTERNAL_SERVER_ERROR),
    USER_NOT_FOUND(1001, "User not found", HttpStatus.NOT_FOUND),
    USERNAME_ALREADY_EXISTS(1002, "Username is already in use", HttpStatus.BAD_REQUEST),
    UNAUTHORIZED(1003, "You do not have permission", HttpStatus.UNAUTHORIZED),
    INVALID_KEY(1004, "Invalid key", HttpStatus.BAD_REQUEST),
    VALIDATION_FAILED(1005, "Validation failed", HttpStatus.BAD_REQUEST),
    ACCESS_DENIED(1006, "Access denied", HttpStatus.FORBIDDEN),
    RESOURCE_NOT_FOUND(1007, "Resource not found", HttpStatus.NOT_FOUND),
    // Friendship
    ALREADY_FRIENDS(2001, "Already friends with this user", HttpStatus.BAD_REQUEST),
    FRIEND_REQUEST_ALREADY_SENT(2002, "Friend request already sent", HttpStatus.BAD_REQUEST),
    CANNOT_SEND_FRIEND_REQUEST(2003, "Cannot send friend request to this user", HttpStatus.BAD_REQUEST),
    NO_PENDING_FRIEND_REQUEST(2004, "No pending friend request from this user", HttpStatus.BAD_REQUEST),
    NO_FRIENDSHIP_FOUND(2005, "No friendship found with this user", HttpStatus.BAD_REQUEST),
    USER_ALREADY_BLOCKED(2006, "User is already blocked", HttpStatus.BAD_REQUEST),
    USER_NOT_BLOCKED(2007, "User is not blocked", HttpStatus.BAD_REQUEST),
    YOU_DID_NOT_BLOCK_USER(2008, "You did not block this user", HttpStatus.BAD_REQUEST),
    CANNOT_REMOVE_BLOCKED_RELATIONSHIP(2009, "Cannot remove a blocked relationship", HttpStatus.BAD_REQUEST),
    YOU_HAVE_BLOCKED_USER(2010, "You have blocked this user. Unblock first", HttpStatus.BAD_REQUEST),
    // Search
    INDEXING_ERROR(3001, "Failed to index data", HttpStatus.INTERNAL_SERVER_ERROR),
    SEARCH_ERROR(3002, "Search operation failed", HttpStatus.INTERNAL_SERVER_ERROR),
    // Storage
    IMAGE_NOT_FOUND(4001, "Image not found", HttpStatus.NOT_FOUND),
    IMAGE_STORAGE_ERROR(4002, "Image storage error", HttpStatus.INTERNAL_SERVER_ERROR);

    private final int code;
    private final String message;
    private final HttpStatus statusCode;

    ErrorCode(int code, String message, HttpStatus statusCode) {
        this.code = code;
        this.message = message;
        this.statusCode = statusCode;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public HttpStatus getStatusCode() {
        return statusCode;
    }
}
