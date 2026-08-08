package com.thechat.security;

import java.time.Duration;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Signing config for service-to-service tokens (Phase 5).
 *
 * Deliberately a SEPARATE secret from app.jwt — the end-user JWT secret and the
 * internal service-token secret must never be the same key. If a service token ever
 * leaked, it should be useless for minting end-user tokens, and vice versa.
 */
@Validated
@ConfigurationProperties(prefix = "app.service-jwt")
public record ServiceJwtProperties(
        @NotBlank String issuer,
        @NotBlank @Size(min = 32) String secret,
        @NotNull Duration ttl
) {
}
