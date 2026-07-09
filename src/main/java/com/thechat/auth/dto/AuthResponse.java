package com.thechat.auth.dto;

public record AuthResponse(
        long expiresInSeconds,
        UserResponse user
) {
}
