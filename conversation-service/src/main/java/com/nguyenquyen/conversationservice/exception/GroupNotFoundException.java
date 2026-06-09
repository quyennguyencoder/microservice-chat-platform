package com.nguyenquyen.conversationservice.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.UUID;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class GroupNotFoundException extends RuntimeException {
    public GroupNotFoundException(UUID groupId) {
        super("Group not found: " + groupId);
    }
    public GroupNotFoundException(String message) {
        super(message);
    }
}
