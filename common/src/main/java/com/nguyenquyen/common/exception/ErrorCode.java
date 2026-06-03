package com.nguyenquyen.common.exception;

public enum ErrorCode {
    UNCATEGORIZED("COMMON_000", "Uncategorized error"),
    VALIDATION_FAILED("COMMON_001", "Validation failed"),
    UNAUTHENTICATED("AUTH_001", "Unauthenticated"),
    UNAUTHORIZED("AUTH_002", "Unauthorized"),
    RESOURCE_NOT_FOUND("COMMON_404", "Resource not found"),
    CONFLICT("COMMON_409", "Resource conflict"),
    INTERNAL_SERVER_ERROR("COMMON_500", "Internal server error");

    private final String code;
    private final String message;

    ErrorCode(String code, String message) {
        this.code = code;
        this.message = message;
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}