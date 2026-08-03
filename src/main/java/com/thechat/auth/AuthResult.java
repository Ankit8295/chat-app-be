package com.thechat.auth;

import com.thechat.auth.dto.AuthResponse;

public record AuthResult(
        String accessToken,
        String refreshToken,
        AuthResponse response
) {
}
