package com.nguyenquyen.userservice.exception;

import com.nguyenquyen.common.exception.BadRequestException;

public class FriendshipException extends BadRequestException {
    public FriendshipException(String message) {
        super(message);
    }
}
