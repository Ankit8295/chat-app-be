package com.thechat.security;

import java.nio.charset.StandardCharsets;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimNames;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import com.nimbusds.jose.jwk.source.ImmutableSecret;

/**
 * Encoder/decoder for the internal service-token secret (Phase 5).
 *
 * Every service gets both beans:
 *   - serviceJwtEncoder — used by callers (Auth -> User, Chat -> User) to mint their own identity token.
 *   - serviceJwtDecoder — used by callees (User's /internal/** filter chain) to validate incoming tokens.
 *
 * Bean names matter here: they're distinct from the unqualified "jwtEncoder"/"jwtDecoder" beans that
 * sign/verify end-user tokens, so Spring disambiguates by parameter name at each injection site instead
 * of throwing NoUniqueBeanDefinitionException.
 */
@Configuration
public class ServiceJwtConfig {

    @Bean
    JwtEncoder serviceJwtEncoder(ServiceJwtProperties serviceJwtProperties) {
        return new NimbusJwtEncoder(new ImmutableSecret<>(secretKey(serviceJwtProperties)));
    }

    @Bean
    JwtDecoder serviceJwtDecoder(ServiceJwtProperties serviceJwtProperties) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(secretKey(serviceJwtProperties))
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
        // JwtValidators.createDefaultWithIssuer(...) is deliberately NOT used here: it calls
        // Jwt.getIssuer(), which parses the "iss" claim as a java.net.URL — but our issuer
        // ("the-chat-internal") is a plain service name, not a URL. Checking the raw claim string
        // avoids that URL-parsing assumption while still rejecting tokens minted for another issuer.
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                JwtValidators.createDefault(),
                issuerClaimValidator(serviceJwtProperties.issuer())));
        return decoder;
    }

    private OAuth2TokenValidator<Jwt> issuerClaimValidator(String expectedIssuer) {
        return jwt -> expectedIssuer.equals(jwt.getClaimAsString(JwtClaimNames.ISS))
                ? OAuth2TokenValidatorResult.success()
                : OAuth2TokenValidatorResult.failure(
                        new OAuth2Error("invalid_token", "Unexpected service token issuer", null));
    }

    private SecretKey secretKey(ServiceJwtProperties serviceJwtProperties) {
        byte[] secretBytes = serviceJwtProperties.secret().getBytes(StandardCharsets.UTF_8);
        return new SecretKeySpec(secretBytes, "HmacSHA256");
    }
}
