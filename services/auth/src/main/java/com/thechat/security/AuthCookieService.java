package com.thechat.security;

import java.time.Duration;

import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;

@Service
public class AuthCookieService {

    private final AuthCookieProperties authCookieProperties;
    private final JwtProperties jwtProperties;

    public AuthCookieService(AuthCookieProperties authCookieProperties, JwtProperties jwtProperties) {
        this.authCookieProperties = authCookieProperties;
        this.jwtProperties = jwtProperties;
    }

    public ResponseCookie createAccessTokenCookie(String accessToken) {
        return baseCookie(authCookieProperties.name(), accessToken)
                .maxAge(jwtProperties.accessTokenTtl())
                .build();
    }

    public ResponseCookie createRefreshTokenCookie(String refreshToken) {
        return baseCookie(authCookieProperties.refreshName(), refreshToken)
                .maxAge(jwtProperties.refreshTokenTtl())
                .build();
    }

    public ResponseCookie clearAccessTokenCookie() {
        return baseCookie(authCookieProperties.name(), "")
                .maxAge(Duration.ZERO)
                .build();
    }

    public ResponseCookie clearRefreshTokenCookie() {
        return baseCookie(authCookieProperties.refreshName(), "")
                .maxAge(Duration.ZERO)
                .build();
    }

    public String readRefreshToken(HttpServletRequest request) {
        return readCookie(request, authCookieProperties.refreshName());
    }

    public String readCookie(HttpServletRequest request, String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        for (Cookie cookie : cookies) {
            if (name.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }

    private ResponseCookie.ResponseCookieBuilder baseCookie(String name, String value) {
        ResponseCookie.ResponseCookieBuilder builder = ResponseCookie.from(name, value)
                .httpOnly(true)
                .secure(authCookieProperties.secure())
                .sameSite(authCookieProperties.sameSite())
                .path(authCookieProperties.path());
        String domain = authCookieProperties.domain();
        if (domain != null && !domain.isBlank()) {
            builder.domain(domain);
        }
        return builder;
    }
}
