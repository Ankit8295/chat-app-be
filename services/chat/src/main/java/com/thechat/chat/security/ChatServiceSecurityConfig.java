package com.thechat.chat.security;

import java.nio.charset.StandardCharsets;
import java.util.List;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.thechat.security.CookieBearerTokenResolver;
import com.thechat.security.CorsProperties;
import com.thechat.security.JwtProperties;

/**
 * Security config for the standalone Chat service (Phase 4).
 *
 * Same pattern as Auth's SecurityConfig and User's UserServiceSecurityConfig:
 *   - No PasswordEncoder / AuthenticationManager — Chat never handles login.
 *   - No JwtEncoder — Chat never issues tokens.
 *   - Only JwtDecoder — validates tokens issued by Auth service (shared HS256 secret).
 *   - /ws/** and /internal/** bypass Spring Security's HTTP filter chain because the
 *     WebSocket handshake is authenticated separately by JwtHandshakeInterceptor,
 *     and /internal/** is blocked at nginx (never publicly reachable).
 */
@Configuration
public class ChatServiceSecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            CookieBearerTokenResolver cookieBearerTokenResolver) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> {
                })
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/health", "/actuator/health/**").permitAll()
                        .requestMatchers("/ws/**").permitAll()
                        .requestMatchers("/internal/**").permitAll()
                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2
                        .bearerTokenResolver(cookieBearerTokenResolver)
                        .jwt(jwt -> {
                        }))
                .build();
    }

    // @Primary because common's ServiceJwtConfig also contributes a JwtDecoder (for validating
    // service tokens, Phase 5) to this same context — this one stays the default for the implicit
    // .jwt(jwt -> {}) lookup below, since Chat only ever validates end-user tokens on this chain.
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
