package com.thechat.user.error;

import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.thechat.common.error.ApiError;
import com.thechat.user.ForbiddenOperationException;
import com.thechat.user.IdentityKeyConflictException;
import com.thechat.user.IdentityKeyNotFoundException;
import com.thechat.user.ProfileImageNotFoundException;
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

    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<ApiError> handleIllegalArgument(
            IllegalArgumentException exception,
            HttpServletRequest request) {
        HttpStatus status = HttpStatus.BAD_REQUEST;
        return ResponseEntity.status(status).body(ApiError.of(
                status.value(), status.getReasonPhrase(),
                exception.getMessage(), request.getRequestURI()));
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

    @ExceptionHandler(ForbiddenOperationException.class)
    ResponseEntity<ApiError> handleForbidden(
            ForbiddenOperationException exception,
            HttpServletRequest request) {
        HttpStatus status = HttpStatus.FORBIDDEN;
        return ResponseEntity.status(status).body(ApiError.of(
                status.value(), status.getReasonPhrase(),
                exception.getMessage(), request.getRequestURI()));
    }

    @ExceptionHandler(ProfileImageNotFoundException.class)
    ResponseEntity<ApiError> handleProfileImageNotFound(
            ProfileImageNotFoundException exception,
            HttpServletRequest request) {
        HttpStatus status = HttpStatus.NOT_FOUND;
        return ResponseEntity.status(status).body(ApiError.of(
                status.value(), status.getReasonPhrase(),
                exception.getMessage(), request.getRequestURI()));
    }

    @ExceptionHandler(IdentityKeyNotFoundException.class)
    ResponseEntity<ApiError> handleIdentityKeyNotFound(
            IdentityKeyNotFoundException exception,
            HttpServletRequest request) {
        HttpStatus status = HttpStatus.NOT_FOUND;
        return ResponseEntity.status(status).body(ApiError.of(
                status.value(), status.getReasonPhrase(),
                exception.getMessage(), request.getRequestURI()));
    }

    @ExceptionHandler(IdentityKeyConflictException.class)
    ResponseEntity<ApiError> handleIdentityKeyConflict(
            IdentityKeyConflictException exception,
            HttpServletRequest request) {
        HttpStatus status = HttpStatus.CONFLICT;
        return ResponseEntity.status(status).body(ApiError.of(
                status.value(), status.getReasonPhrase(),
                exception.getMessage(), request.getRequestURI()));
    }
}
