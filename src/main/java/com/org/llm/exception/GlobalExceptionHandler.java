package com.org.llm.exception;

import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Translates exceptions into consistent {@link ApiError} JSON responses with appropriate HTTP codes.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** Bean-validation failures on {@code @Valid} request bodies. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        for (FieldError fe : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.put(fe.getField(), fe.getDefaultMessage());
        }
        return build(HttpStatus.BAD_REQUEST, "Validation failed", "One or more fields are invalid", fieldErrors);
    }

    /** Bean-validation failures on {@code @Validated} method parameters. */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiError> handleConstraint(ConstraintViolationException ex) {
        return build(HttpStatus.BAD_REQUEST, "Validation failed", ex.getMessage(), null);
    }

    /** Malformed / empty JSON body. */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> handleUnreadable(HttpMessageNotReadableException ex) {
        return build(HttpStatus.BAD_REQUEST, "Malformed request", "Request body is missing or not valid JSON", null);
    }

    /** Bad arguments — e.g. unsupported audio type, disallowed SQL, unknown model output. */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> handleIllegalArgument(IllegalArgumentException ex) {
        return build(HttpStatus.BAD_REQUEST, "Bad request", ex.getMessage(), null);
    }

    /** Upload exceeded the configured multipart limit. */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiError> handleMaxUpload(MaxUploadSizeExceededException ex) {
        return build(HttpStatus.PAYLOAD_TOO_LARGE, "Upload too large",
                "The uploaded file exceeds the maximum allowed size", null);
    }

    /** File / IO failures (PDF reading, audio handling). */
    @ExceptionHandler(IOException.class)
    public ResponseEntity<ApiError> handleIo(IOException ex) {
        log.error("IO error handling request", ex);
        return build(HttpStatus.BAD_GATEWAY, "IO error", ex.getMessage(), null);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleGeneric(Exception ex) {
        log.error("Unhandled error", ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Internal error",
                "An internal error occurred. Please try again.", null);
    }

    private ResponseEntity<ApiError> build(HttpStatus status, String error, String message,
                                           Map<String, String> fieldErrors) {
        return ResponseEntity.status(status)
                .body(ApiError.of(status.value(), error, message, fieldErrors));
    }
}
