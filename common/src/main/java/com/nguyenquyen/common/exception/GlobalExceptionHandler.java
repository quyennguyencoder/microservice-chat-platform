package com.nguyenquyen.common.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ErrorResponse> handleBadRequestException(BadRequestException exception) {
        log.warn("Bad request: {}", exception.getMessage());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(
                        OffsetDateTime.now(),
                        "Bad Request",
                        exception.getMessage(),
                        Collections.singletonList(exception.getMessage())
                ));
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFoundException(ResourceNotFoundException exception) {
        log.warn("Resource not found: {}", exception.getMessage());
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse(
                        OffsetDateTime.now(),
                        "Not Found",
                        exception.getMessage(),
                        Collections.singletonList(exception.getMessage())
                ));
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ErrorResponse> handleUnauthorizedException(UnauthorizedException exception) {
        log.warn("Unauthorized access: {}", exception.getMessage());
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(new ErrorResponse(
                        OffsetDateTime.now(),
                        "Unauthorized",
                        exception.getMessage(),
                        Collections.singletonList(exception.getMessage())
                ));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException ex) {
        List<String> details = ex.getBindingResult().getFieldErrors()
                .stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .toList();

        log.debug("Validation failed: {}", details);
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(
                        OffsetDateTime.now(),
                        "Validation Failed",
                        "Invalid request data",
                        details
                ));
    }

    // You can also add generic handling for Spring Security's AccessDeniedException if present on the classpath
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception exception) {
        // If it's a Spring Security AccessDeniedException, handle it properly without explicitly depending on it
        if (exception.getClass().getName().equals("org.springframework.security.access.AccessDeniedException")) {
            log.warn("Access denied: {}", exception.getMessage());
            return ResponseEntity
                    .status(HttpStatus.FORBIDDEN)
                    .body(new ErrorResponse(
                            OffsetDateTime.now(),
                            "Forbidden",
                            exception.getMessage(),
                            Collections.singletonList(exception.getMessage())
                    ));
        }

        log.error("Unexpected error occurred", exception);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse(
                        OffsetDateTime.now(),
                        "Internal Server Error",
                        "Unexpected error occurred",
                        Collections.singletonList("An internal error occurred. Please contact support.")
                ));
    }
}
