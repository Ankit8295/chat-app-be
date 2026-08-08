package com.thechat.security;

import java.time.Duration;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimNames;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Round-trips a service token through the real encoder/decoder beans (no Spring context needed —
 * ServiceJwtConfig's @Bean methods are plain factory methods we can call directly), proving:
 *   1. A token issued by ServiceTokenIssuer decodes with the matching secret and carries the
 *      expected identity (subject = caller's service name) and authorization (scope = internal) claims.
 *   2. A token signed with a different secret is rejected — this is the property that makes the
 *      service secret meaningfully different from the end-user JWT secret.
 */
class ServiceTokenIssuerTest {

    private static final String SERVICE_NAME = "auth-service";

    private final ServiceJwtConfig serviceJwtConfig = new ServiceJwtConfig();

    private final ServiceJwtProperties properties = new ServiceJwtProperties(
            "the-chat-internal",
            "unit-test-service-secret-at-least-32-characters-long",
            Duration.ofMinutes(1));

    @Test
    void issuedTokenDecodesWithExpectedClaims() {
        JwtEncoder encoder = serviceJwtConfig.serviceJwtEncoder(properties);
        JwtDecoder decoder = serviceJwtConfig.serviceJwtDecoder(properties);
        ServiceTokenIssuer issuer = new ServiceTokenIssuer(encoder, properties, SERVICE_NAME);

        String token = issuer.issue();
        Jwt decoded = decoder.decode(token);

        assertThat(decoded.getSubject()).isEqualTo(SERVICE_NAME);
        assertThat(decoded.getClaimAsString("scope")).isEqualTo("internal");
        assertThat(decoded.getClaimAsString(JwtClaimNames.ISS)).isEqualTo(properties.issuer());
        assertThat(decoded.getExpiresAt()).isAfter(decoded.getIssuedAt());
    }

    @Test
    void tokenSignedWithDifferentSecretIsRejected() {
        JwtEncoder encoder = serviceJwtConfig.serviceJwtEncoder(properties);
        ServiceTokenIssuer issuer = new ServiceTokenIssuer(encoder, properties, SERVICE_NAME);
        String token = issuer.issue();

        ServiceJwtProperties wrongSecretProperties = new ServiceJwtProperties(
                properties.issuer(),
                "a-completely-different-secret-that-is-also-32-plus-chars",
                properties.ttl());
        JwtDecoder decoderWithWrongSecret = serviceJwtConfig.serviceJwtDecoder(wrongSecretProperties);

        assertThatThrownBy(() -> decoderWithWrongSecret.decode(token))
                .isInstanceOf(JwtException.class);
    }
}
