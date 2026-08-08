package com.thechat.security;

import java.time.Instant;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Component;

/**
 * Mints short-lived service-identity tokens (Phase 5).
 *
 * Every outgoing internal call (Auth -> User, Chat -> User) attaches a fresh token minted here.
 * Tokens are intentionally short-lived (see app.service-jwt.ttl, typically ~60s) and minted per
 * request rather than cached: HMAC signing is cheap, and a short TTL keeps the blast radius of a
 * leaked token (e.g. captured in a proxy log) small. Caching with a refresh buffer is a reasonable
 * future optimization once token issuance shows up in a profiler, not before.
 */
@Component
public class ServiceTokenIssuer {

    private static final String SCOPE_CLAIM = "scope";
    private static final String INTERNAL_SCOPE = "internal";

    private final JwtEncoder serviceJwtEncoder;
    private final ServiceJwtProperties serviceJwtProperties;
    private final String serviceName;

    public ServiceTokenIssuer(
            @Qualifier("serviceJwtEncoder") JwtEncoder serviceJwtEncoder,
            ServiceJwtProperties serviceJwtProperties,
            @Value("${spring.application.name}") String serviceName) {
        this.serviceJwtEncoder = serviceJwtEncoder;
        this.serviceJwtProperties = serviceJwtProperties;
        this.serviceName = serviceName;
    }

    public String issue() {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(serviceJwtProperties.issuer())
                .subject(serviceName)
                .issuedAt(now)
                .expiresAt(now.plus(serviceJwtProperties.ttl()))
                .claim(SCOPE_CLAIM, INTERNAL_SCOPE)
                .build();

        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        return serviceJwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }
}
