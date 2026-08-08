package com.thechat.auth.dto;

/**
 * Minimal auth response — Auth service does not own profile data (name, image).
 * Callers should fetch /api/v1/users/me for profile after authentication.
 */
public record AuthResponse(long expiresInSeconds) {
}
