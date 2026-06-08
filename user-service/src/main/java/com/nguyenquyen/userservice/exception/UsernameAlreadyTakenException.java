package com.nguyenquyen.userservice.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class UsernameAlreadyTakenException extends RuntimeException {

    public UsernameAlreadyTakenException(String username) {
        super("Username is already taken: @" + username);
    }
}
