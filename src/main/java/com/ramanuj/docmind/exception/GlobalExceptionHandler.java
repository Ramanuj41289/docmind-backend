
package com.ramanuj.docmind.exception;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import com.ramanuj.docmind.dto.ApiResponse;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger =
            LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotFound(
            ResourceNotFoundException ex) {

        logger.warn("Resource not found: {}", ex.getMessage());

        return build(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(DocumentProcessingException.class)
    public ResponseEntity<ApiResponse<Void>> handleProcessingError(
            DocumentProcessingException ex) {

        logger.error("Document processing failed", ex);

        return build(
                HttpStatus.UNPROCESSABLE_CONTENT,
                "Failed to process document"
        );
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiResponse<Void>> handleMaxSize(
            MaxUploadSizeExceededException ex) {

        logger.warn("File size limit exceeded");

        return build(
                HttpStatus.CONTENT_TOO_LARGE,
                "File size exceeds the allowed limit of 25MB"
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleValidation(
            MethodArgumentNotValidException ex) {

        Map<String, String> errors = new LinkedHashMap<>();

        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            errors.putIfAbsent(
                    error.getField(),
                    error.getDefaultMessage()
            );
        }

        return ResponseEntity.badRequest()
                .body(ApiResponse.<Map<String, String>>builder()
                        .success(false)
                        .message("Validation failed")
                        .data(errors)
                        .timestamp(LocalDateTime.now())
                        .build());
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDenied(
            AccessDeniedException ex) {

        logger.warn("Access denied: {}", ex.getMessage());

        return build(
                HttpStatus.FORBIDDEN,
                "You do not have permission to access this resource"
        );
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiResponse<Void>> handleAuthentication(
            AuthenticationException ex) {

        logger.warn("Authentication failed: {}",
                ex.getClass().getSimpleName());

        return build(
                HttpStatus.UNAUTHORIZED,
                "Invalid username or password"
        );
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleDataIntegrity(
            DataIntegrityViolationException ex) {

        logger.warn("Database constraint violation", ex);

        return build(
                HttpStatus.CONFLICT,
                "A record with the same unique value already exists"
        );
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalArgument(
            IllegalArgumentException ex) {

        logger.warn("Invalid request: {}", ex.getMessage());

        return build(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGeneric(Exception ex) {

        logger.error("Unexpected server error", ex);

        return build(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected error occurred"
        );
    }

    private ResponseEntity<ApiResponse<Void>> build(
            HttpStatus status, String message) {

        return ResponseEntity.status(status)
                .body(ApiResponse.<Void>builder()
                        .success(false)
                        .message(message)
                        .data(null)
                        .timestamp(LocalDateTime.now())
                        .build());
    }
    
    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ApiResponse<Void>> handleConflictException(
            ConflictException ex) {

        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResponse.<Void>builder()
                        .success(false)
                        .message(ex.getMessage())
                        .data(null)
                        .timestamp(LocalDateTime.now())
                        .build());
    }
}
