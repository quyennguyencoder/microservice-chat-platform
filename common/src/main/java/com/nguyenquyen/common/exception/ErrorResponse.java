package com.nguyenquyen.common.exception;

import java.time.OffsetDateTime;
import java.util.List;

public record ErrorResponse(
    OffsetDateTime timestamp,
    String error,
    String message,
    List<String> details
) {
}
