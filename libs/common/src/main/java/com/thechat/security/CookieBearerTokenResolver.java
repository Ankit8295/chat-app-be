package com.thechat.security;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;
import org.springframework.stereotype.Component;

@Component
public class CookieBearerTokenResolver implements BearerTokenResolver {

    private final AuthCookieProperties authCookieProperties;

    public CookieBearerTokenResolver(AuthCookieProperties authCookieProperties) {
        this.authCookieProperties = authCookieProperties;
    }

    @Override
    public String resolve(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }

        for (Cookie cookie : cookies) {
            if (authCookieProperties.name().equals(cookie.getName())) {
                return cookie.getValue();
            }
        }

        return null;
    }
}
