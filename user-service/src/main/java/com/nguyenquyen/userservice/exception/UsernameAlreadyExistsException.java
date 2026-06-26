package com.nguyenquyen.userservice.exception;


import com.nguyenquyen.common.exception.BadRequestException;

public class UsernameAlreadyExistsException extends BadRequestException {
    public UsernameAlreadyExistsException(String message) {
        super(message);
    }
}
