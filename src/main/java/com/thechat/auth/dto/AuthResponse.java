package com.thechat.auth.dto;

import com.thechat.user.UserResponse;

public record AuthResponse(
                long expiresInSeconds,
                UserResponse user) {
}
