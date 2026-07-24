package com.nguyenquyen.storageservice.exception;

import com.nguyenquyen.common.exception.ResourceNotFoundException;

public class ImageNotFoundException extends ResourceNotFoundException {

    public ImageNotFoundException(String message) {
        super(message);
    }
}