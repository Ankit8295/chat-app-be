package com.thechat.security;

import java.time.Duration;

import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;

@Service
public class AuthCookieService {

    private final AuthCookieProperties authCookieProperties;
    private final JwtProperties jwtProperties;

    public AuthCookieService(AuthCookieProperties authCookieProperties, JwtProperties jwtProperties) {
        this.authCookieProperties = authCookieProperties;
        this.jwtProperties = jwtProperties;
    }

    public ResponseCookie createAccessTokenCookie(String accessToken) {
        return baseCookie(accessToken)
                .maxAge(jwtProperties.accessTokenTtl())
                .build();
    }

    public ResponseCookie clearAccessTokenCookie() {
        return baseCookie("")
                .maxAge(Duration.ZERO)
                .build();
    }

    private ResponseCookie.ResponseCookieBuilder baseCookie(String value) {
        return ResponseCookie.from(authCookieProperties.name(), value)
                .httpOnly(true)
                .secure(authCookieProperties.secure())
                .sameSite(authCookieProperties.sameSite())
                .path(authCookieProperties.path());
    }
}
