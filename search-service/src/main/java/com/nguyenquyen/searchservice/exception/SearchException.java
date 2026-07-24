package com.nguyenquyen.searchservice.exception;

import com.nguyenquyen.common.exception.BadRequestException;

public class SearchException extends BadRequestException {

    public SearchException(String message) {
        super(message);
    }

    public SearchException(String message, Throwable cause) {
        super(message, cause);
    }
}