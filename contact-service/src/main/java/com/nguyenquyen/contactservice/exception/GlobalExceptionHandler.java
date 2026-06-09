package com.nguyenquyen.contactservice.exception;

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
 * Global exception handler providing consistent JSON error responses
 * across all Contact Service endpoints.
 *
 * <p>All error responses follow the standard BizChat error shape:</p>
 * <pre>
 * {
 *   "timestamp": "2026-01-01T10:00:00",
 *   "status": 400,
 *   "error": "Bad Request",
 *   "message": "Human-readable reason",
 *   "path": "/api/contacts/..."
 * }
 * </pre>
 *
 * @author  Aniket Kamlesh
 * @version 1.0.0
 * @since   1.0.0
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<Map<String, Object>> handleMissingHeader(
            MissingRequestHeaderException ex, HttpServletRequest request) {
        return ResponseEntity.badRequest().body(
                errorBody(400, "Bad Request", ex.getMessage(), request.getRequestURI()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(
            MethodArgumentNotValidException ex, HttpServletRequest request) {
        Map<String, String> fieldErrors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error ->
            fieldErrors.put(((FieldError) error).getField(), error.getDefaultMessage()));
        return ResponseEntity.badRequest().body(
                errorBody(400, "Validation Failed", fieldErrors.toString(), request.getRequestURI()));
    }

    @ExceptionHandler(ContactNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(
            ContactNotFoundException ex, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                errorBody(404, "Not Found", ex.getMessage(), request.getRequestURI()));
    }

    @ExceptionHandler(ContactAlreadyExistsException.class)
    public ResponseEntity<Map<String, Object>> handleConflict(
            ContactAlreadyExistsException ex, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(
                errorBody(409, "Conflict", ex.getMessage(), request.getRequestURI()));
    }

    @ExceptionHandler(ContactAccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> handleAccessDenied(
            ContactAccessDeniedException ex, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                errorBody(403, "Forbidden", ex.getMessage(), request.getRequestURI()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(
            IllegalArgumentException ex, HttpServletRequest request) {
        return ResponseEntity.badRequest().body(
                errorBody(400, "Bad Request", ex.getMessage(), request.getRequestURI()));
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
        body.put("timestamp", Instant.now().toString());
        body.put("status", status);
        body.put("error", error);
        body.put("message", message);
        body.put("path", path);
        return body;
    }
}
