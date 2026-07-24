package com.nguyenquyen.storageservice.exception;

import com.nguyenquyen.common.exception.BadRequestException;

public class ImageStorageException extends BadRequestException {

    public ImageStorageException(String message) {
        super(message);
    }

    public ImageStorageException(String message, Throwable cause) {
        super(message, cause);
    }
}
