package com.thechat.auth;

import java.time.Instant;
import java.util.UUID;

public record RefreshTokenSession(
        UUID userId,
        String deviceId,
        Instant createdAt,
        Instant expiresAt,
        boolean revoked) {
}
