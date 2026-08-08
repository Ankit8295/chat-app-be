package com.thechat.user.security;

import java.nio.charset.StandardCharsets;
import java.util.List;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.web.DefaultBearerTokenResolver;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.thechat.security.CookieBearerTokenResolver;
import com.thechat.security.CorsProperties;
import com.thechat.security.JwtProperties;

/**
 * Security config for the User service — TWO independent filter chains (Phase 5):
 *
 *   1. internalSecurityFilterChain (@Order 1, matches /internal/**)
 *      Validates the caller's SERVICE token (separate secret, see ServiceJwtConfig in common) and
 *      requires the "internal" scope. Only Auth and Chat — the only services holding the service
 *      secret — can call these routes. Nothing else is permitted, not even a valid end-user token.
 *
 *   2. publicSecurityFilterChain (@Order 2, everything else)
 *      Validates end-user JWTs issued by Auth service, unchanged from before Phase 5.
 *
 * Spring tries chains in @Order and picks the first whose securityMatcher matches the request, so
 * /internal/** never falls through to the end-user chain (and vice versa) even though both chains
 * are of type JwtDecoder-backed oauth2ResourceServer.
 */
@Configuration
public class UserServiceSecurityConfig {

    @Bean
    @Order(1)
    SecurityFilterChain internalSecurityFilterChain(
            HttpSecurity http,
            @Qualifier("serviceJwtDecoder") JwtDecoder serviceJwtDecoder) throws Exception {
        return http
                .securityMatcher("/internal/**")
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/internal/**").hasAuthority("SCOPE_internal")
                        .anyRequest().denyAll())
                .oauth2ResourceServer(oauth2 -> oauth2
                        // Without this, Spring Security falls back to whatever BearerTokenResolver
                        // bean it finds in the context — which here is CookieBearerTokenResolver
                        // (used by the public chain below). Service calls send a standard
                        // "Authorization: Bearer <token>" header, not a cookie, so this chain needs
                        // its own resolver explicitly instead of inheriting that fallback.
                        .bearerTokenResolver(new DefaultBearerTokenResolver())
                        .jwt(jwt -> jwt.decoder(serviceJwtDecoder)))
                .build();
    }

    @Bean
    @Order(2)
    SecurityFilterChain publicSecurityFilterChain(
            HttpSecurity http,
            CookieBearerTokenResolver cookieBearerTokenResolver,
            @Qualifier("jwtDecoder") JwtDecoder jwtDecoder) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> {
                })
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/health", "/actuator/health/**").permitAll()
                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2
                        .bearerTokenResolver(cookieBearerTokenResolver)
                        .jwt(jwt -> jwt.decoder(jwtDecoder)))
                .build();
    }

    @Bean
    @Primary
    JwtDecoder jwtDecoder(JwtProperties jwtProperties) {
        byte[] secretBytes = jwtProperties.secret().getBytes(StandardCharsets.UTF_8);
        SecretKey key = new SecretKeySpec(secretBytes, "HmacSHA256");
        return NimbusJwtDecoder.withSecretKey(key)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource(CorsProperties corsProperties) {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(corsProperties.allowedOrigins());
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Content-Type"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
