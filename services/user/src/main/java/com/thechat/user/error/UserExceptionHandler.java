package com.thechat.user.error;

import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.thechat.common.error.ApiError;
import com.thechat.user.UserNotFoundException;

import jakarta.servlet.http.HttpServletRequest;

@RestControllerAdvice
public class UserExceptionHandler {

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

    @ExceptionHandler(UserNotFoundException.class)
    ResponseEntity<ApiError> handleUserNotFound(
            UserNotFoundException exception,
            HttpServletRequest request) {
        HttpStatus status = HttpStatus.NOT_FOUND;
        return ResponseEntity.status(status).body(ApiError.of(
                status.value(), status.getReasonPhrase(),
                exception.getMessage(), request.getRequestURI()));
    }
}
