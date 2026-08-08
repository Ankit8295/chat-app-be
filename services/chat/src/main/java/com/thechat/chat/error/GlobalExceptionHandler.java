package com.thechat.chat.error;

import com.thechat.common.error.ApiError;
import com.thechat.conversation.ConversationNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Global exception handler for the standalone Chat service (Phase 4).
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiError> handleValidation(
            MethodArgumentNotValidException exception,
            HttpServletRequest request) {
        Map<String, String> fieldErrors = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .collect(Collectors.toMap(
                        fe -> fe.getField(),
                        fe -> fe.getDefaultMessage() == null ? "Invalid value" : fe.getDefaultMessage(),
                        (existing, ignored) -> existing));

        HttpStatus status = HttpStatus.BAD_REQUEST;
        return ResponseEntity.status(status).body(ApiError.withFieldErrors(
                status.value(), status.getReasonPhrase(),
                "Request validation failed", request.getRequestURI(), fieldErrors));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<ApiError> handleIllegalArgument(
            IllegalArgumentException exception,
            HttpServletRequest request) {
        HttpStatus status = HttpStatus.BAD_REQUEST;
        return ResponseEntity.status(status).body(ApiError.of(
                status.value(), status.getReasonPhrase(),
                exception.getMessage(), request.getRequestURI()));
    }

    @ExceptionHandler(ConversationNotFoundException.class)
    ResponseEntity<ApiError> handleConversationNotFound(
            ConversationNotFoundException exception,
            HttpServletRequest request) {
        HttpStatus status = HttpStatus.NOT_FOUND;
        return ResponseEntity.status(status).body(ApiError.of(
                status.value(), status.getReasonPhrase(),
                exception.getMessage(), request.getRequestURI()));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<ApiError> handleDataIntegrityViolation(HttpServletRequest request) {
        HttpStatus status = HttpStatus.CONFLICT;
        return ResponseEntity.status(status).body(ApiError.of(
                status.value(), status.getReasonPhrase(),
                "Request conflicts with existing data", request.getRequestURI()));
    }
}
