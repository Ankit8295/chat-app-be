package com.thechat.urlshortener;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.thechat.security.CookieBearerTokenResolver;
import com.thechat.urlshortener.dto.ShortenUrlRequest;
import com.thechat.urlshortener.dto.UrlResponse;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

@RestController
@Validated
public class UrlController {

    private final UrlService urlService;
    private final CookieBearerTokenResolver cookieBearerTokenResolver;
    private final JwtDecoder jwtDecoder;

    public UrlController(
            UrlService urlService,
            CookieBearerTokenResolver cookieBearerTokenResolver,
            JwtDecoder jwtDecoder) {
        this.urlService = urlService;
        this.cookieBearerTokenResolver = cookieBearerTokenResolver;
        this.jwtDecoder = jwtDecoder;
    }

    @PostMapping("/api/v1/urls")
    public ResponseEntity<UrlResponse> shorten(
            @Valid @RequestBody ShortenUrlRequest request,
            HttpServletRequest httpRequest) {
        UUID userId = optionalUserId(httpRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(urlService.shorten(request.longUrl(), userId));
    }

    @GetMapping("/api/v1/urls/me")
    public List<UrlResponse> listMine(@AuthenticationPrincipal Jwt jwt) {
        return urlService.listMine(requireUserId(jwt));
    }

    @DeleteMapping("/api/v1/urls/{id}")
    public ResponseEntity<Void> deleteMine(
            @PathVariable UUID id,
            @AuthenticationPrincipal Jwt jwt) {
        urlService.deleteMine(id, requireUserId(jwt));
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/s/{shortCode}")
    public ResponseEntity<Void> redirect(@PathVariable String shortCode) {
        String longUrl = urlService.getLongUrl(shortCode);
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(longUrl))
                .build();
    }

    /**
     * Public shorten chain has no oauth2ResourceServer, so identity is optional:
     * valid cookie JWT → owned link; missing/invalid cookie → anonymous.
     */
    private UUID optionalUserId(HttpServletRequest request) {
        String token = cookieBearerTokenResolver.resolve(request);
        if (token == null || token.isBlank()) {
            return null;
        }
        try {
            Jwt jwt = jwtDecoder.decode(token);
            return extractUserIdOrNull(jwt);
        } catch (JwtException | IllegalArgumentException ignored) {
            return null;
        }
    }

    private static UUID extractUserIdOrNull(Jwt jwt) {
        if (jwt == null) {
            return null;
        }
        String userId = jwt.getClaimAsString("userId");
        if (userId == null || userId.isBlank()) {
            return null;
        }
        return UUID.fromString(userId);
    }

    private static UUID requireUserId(Jwt jwt) {
        UUID userId = extractUserIdOrNull(jwt);
        if (userId == null) {
            throw new IllegalArgumentException("Authenticated user required");
        }
        return userId;
    }
}
