package com.nguyenquyen.searchservice.exception;

import com.nguyenquyen.common.exception.BadRequestException;

public class IndexingException extends BadRequestException {

    public IndexingException(String message) {
        super(message);
    }

    public IndexingException(String message, Throwable cause) {
        super(message, cause);
    }
}