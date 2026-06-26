package com.nguyenquyen.userservice.exception;

import com.nguyenquyen.common.exception.ResourceNotFoundException;

public class UserNotFoundException extends ResourceNotFoundException {
    public UserNotFoundException(String message) {
        super(message);
    }
}
