package com.nguyenquyen.userservice.exception;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Global Exception Handler — consistent error format across all endpoints
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    // ─── Missing required @RequestHeader → 400 ────────────────
    // Without this, Spring's generic Exception handler catches it → 500.
    // @ExceptionHandler on more specific type takes priority over Exception.class.
    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<Map<String, Object>> handleMissingHeader(
            MissingRequestHeaderException ex, HttpServletRequest request) {
        return ResponseEntity.badRequest().body(
                errorBody(400, "Bad Request", ex.getMessage(), request.getRequestURI()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationErrors(
            MethodArgumentNotValidException ex, HttpServletRequest request) {

        Map<String, String> fieldErrors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            fieldErrors.put(fieldName, error.getDefaultMessage());
        });

        return ResponseEntity.badRequest().body(
                errorBody(400, "Validation Failed", fieldErrors.toString(), request.getRequestURI()));
    }

    @ExceptionHandler(ProfileNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(
            ProfileNotFoundException ex, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                errorBody(404, "Not Found", ex.getMessage(), request.getRequestURI()));
    }

    @ExceptionHandler(UsernameAlreadyTakenException.class)
    public ResponseEntity<Map<String, Object>> handleUsernameConflict(
            UsernameAlreadyTakenException ex, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(
                errorBody(409, "Conflict", ex.getMessage(), request.getRequestURI()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneric(
            Exception ex, HttpServletRequest request) {
        log.error("Unexpected error at {}: ", request.getRequestURI(), ex);
        return ResponseEntity.internalServerError().body(
                errorBody(500, "Internal Server Error", "An unexpected error occurred", request.getRequestURI()));
    }

    private Map<String, Object> errorBody(int status, String error, String message, String path) {
        Map<String, Object> body = new HashMap<>();
        body.put("status", status);
        body.put("error", error);
        body.put("message", message);
        body.put("path", path);
        body.put("timestamp", Instant.now().toString());
        return body;
    }
}
