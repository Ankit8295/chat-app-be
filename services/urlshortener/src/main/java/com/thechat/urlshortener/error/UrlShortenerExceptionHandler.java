package com.thechat.urlshortener.error;

import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.thechat.common.error.ApiError;
import com.thechat.urlshortener.UrlAccessDeniedException;
import com.thechat.urlshortener.UrlNotFoundException;

import jakarta.servlet.http.HttpServletRequest;

@RestControllerAdvice
public class UrlShortenerExceptionHandler {

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

    @ExceptionHandler(UrlNotFoundException.class)
    ResponseEntity<ApiError> handleNotFound(
            UrlNotFoundException exception,
            HttpServletRequest request) {
        HttpStatus status = HttpStatus.NOT_FOUND;
        return ResponseEntity.status(status).body(ApiError.of(
                status.value(), status.getReasonPhrase(),
                exception.getMessage(), request.getRequestURI()));
    }

    @ExceptionHandler(UrlAccessDeniedException.class)
    ResponseEntity<ApiError> handleAccessDenied(
            UrlAccessDeniedException exception,
            HttpServletRequest request) {
        HttpStatus status = HttpStatus.FORBIDDEN;
        return ResponseEntity.status(status).body(ApiError.of(
                status.value(), status.getReasonPhrase(),
                exception.getMessage(), request.getRequestURI()));
    }
}
