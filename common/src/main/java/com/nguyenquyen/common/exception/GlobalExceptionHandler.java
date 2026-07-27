package com.nguyenquyen.common.exception;

import com.nguyenquyen.common.dto.response.ApiResponse;
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

        @ExceptionHandler(AppException.class)
        public ResponseEntity<ApiResponse<Void>> handleAppException(AppException exception) {
                log.warn("AppException: {}", exception.getMessage());
                ErrorCode errorCode = exception.getErrorCode();
                return ResponseEntity
                                .status(errorCode.getStatusCode())
                                .body(ApiResponse.error(errorCode.getCode(), exception.getMessage()));
        }

        @ExceptionHandler(BadRequestException.class)
        public ResponseEntity<ApiResponse<Void>> handleBadRequestException(BadRequestException exception) {
                log.warn("Bad request: {}", exception.getMessage());
                return ResponseEntity
                                .status(HttpStatus.BAD_REQUEST)
                                .body(ApiResponse.error(HttpStatus.BAD_REQUEST.value(), exception.getMessage()));
        }

        @ExceptionHandler(ResourceNotFoundException.class)
        public ResponseEntity<ApiResponse<Void>> handleResourceNotFoundException(ResourceNotFoundException exception) {
                log.warn("Resource not found: {}", exception.getMessage());
                return ResponseEntity
                                .status(HttpStatus.NOT_FOUND)
                                .body(ApiResponse.error(HttpStatus.NOT_FOUND.value(), exception.getMessage()));
        }

        @ExceptionHandler(UnauthorizedException.class)
        public ResponseEntity<ApiResponse<Void>> handleUnauthorizedException(UnauthorizedException exception) {
                log.warn("Unauthorized access: {}", exception.getMessage());
                return ResponseEntity
                                .status(HttpStatus.UNAUTHORIZED)
                                .body(ApiResponse.error(HttpStatus.UNAUTHORIZED.value(), exception.getMessage()));
        }

        @ExceptionHandler(MethodArgumentNotValidException.class)
        public ResponseEntity<ApiResponse<Void>> handleValidationException(MethodArgumentNotValidException ex) {
                List<String> details = ex.getBindingResult().getFieldErrors()
                                .stream()
                                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                                .toList();

                log.debug("Validation failed: {}", details);
                // Can create a specific error message format for validation if needed
                return ResponseEntity
                                .status(HttpStatus.BAD_REQUEST)
                                .body(ApiResponse.error(ErrorCode.VALIDATION_FAILED.getCode(), "Validation Failed: " + String.join(", ", details)));
        }

        @ExceptionHandler(Exception.class)
        public ResponseEntity<ApiResponse<Void>> handleGenericException(Exception exception) {
                if (exception.getClass().getName()
                                .equals("org.springframework.security.access.AccessDeniedException")) {
                        log.warn("Access denied: {}", exception.getMessage());
                        return ResponseEntity
                                        .status(HttpStatus.FORBIDDEN)
                                        .body(ApiResponse.error(ErrorCode.ACCESS_DENIED.getCode(), "Forbidden"));
                }

                String exceptionName = exception.getClass().getSimpleName();
                if (exceptionName.equals("ClientAbortException") || 
                    exceptionName.equals("AsyncRequestNotUsableException") ||
                    (exception instanceof java.io.IOException && exception.getMessage() != null && 
                     (exception.getMessage().contains("aborted") || exception.getMessage().contains("Broken pipe")))) {
                        log.debug("Client disconnected during async request: {}", exception.getMessage());
                        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
                }

                log.error("Unexpected error occurred", exception);
                return ResponseEntity
                                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                                .body(ApiResponse.error(ErrorCode.UNCATEGORIZED_EXCEPTION.getCode(), ErrorCode.UNCATEGORIZED_EXCEPTION.getMessage()));
        }
}
