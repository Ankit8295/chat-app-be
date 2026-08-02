package com.thechat.ws;

import java.util.Map;
import java.util.UUID;

import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import com.thechat.security.CookieBearerTokenResolver;

import jakarta.servlet.http.HttpServletRequest;

@Component
public class JwtHandshakeInterceptor implements HandshakeInterceptor {

    public static final String USER_ID_ATTR = "userId";

    private final CookieBearerTokenResolver cookieBearerTokenResolver;
    private final JwtDecoder jwtDecoder;

    public JwtHandshakeInterceptor(
            CookieBearerTokenResolver cookieBearerTokenResolver,
            JwtDecoder jwtDecoder) {
        this.cookieBearerTokenResolver = cookieBearerTokenResolver;
        this.jwtDecoder = jwtDecoder;
    }

    @Override
    public boolean beforeHandshake(
            ServerHttpRequest request,
            ServerHttpResponse response,
            WebSocketHandler wsHandler,
            Map<String, Object> attributes) {
        if (!(request instanceof ServletServerHttpRequest servletRequest)) {
            return false;
        }

        HttpServletRequest httpRequest = servletRequest.getServletRequest();
        String token = cookieBearerTokenResolver.resolve(httpRequest);
        if (token == null || token.isBlank()) {
            return false;
        }

        try {
            Jwt jwt = jwtDecoder.decode(token);
            String userIdClaim = jwt.getClaimAsString("userId");
            if (userIdClaim == null || userIdClaim.isBlank()) {
                return false;
            }
            attributes.put(USER_ID_ATTR, UUID.fromString(userIdClaim));
            return true;
        } catch (JwtException | IllegalArgumentException ex) {
            return false;
        }
    }

    @Override
    public void afterHandshake(
            ServerHttpRequest request,
            ServerHttpResponse response,
            WebSocketHandler wsHandler,
            Exception exception) {
        // no-op
    }
}
