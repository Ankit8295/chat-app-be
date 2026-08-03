package com.thechat.auth;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.thechat.auth.dto.AuthResponse;
import com.thechat.auth.dto.LoginRequest;
import com.thechat.auth.dto.RegisterRequest;
import com.thechat.security.AuthCookieService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;
    private final AuthCookieService authCookieService;

    public AuthController(AuthService authService, AuthCookieService authCookieService) {
        this.authService = authService;
        this.authCookieService = authCookieService;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        AuthResult authResult = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .headers(authCookies(authResult))
                .body(authResult.response());
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthResult authResult = authService.login(request);
        return ResponseEntity.ok()
                .headers(authCookies(authResult))
                .body(authResult.response());
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(HttpServletRequest request) {
        String refreshToken = authCookieService.readRefreshToken(request);
        AuthResult authResult = authService.refresh(refreshToken);
        return ResponseEntity.ok()
                .headers(authCookies(authResult))
                .body(authResult.response());
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request) {
        authService.logout(authCookieService.readRefreshToken(request));
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.SET_COOKIE, authCookieService.clearAccessTokenCookie().toString());
        headers.add(HttpHeaders.SET_COOKIE, authCookieService.clearRefreshTokenCookie().toString());
        return ResponseEntity.noContent().headers(headers).build();
    }

    private HttpHeaders authCookies(AuthResult authResult) {
        HttpHeaders headers = new HttpHeaders();
        headers.add(
                HttpHeaders.SET_COOKIE,
                authCookieService.createAccessTokenCookie(authResult.accessToken()).toString());
        headers.add(
                HttpHeaders.SET_COOKIE,
                authCookieService.createRefreshTokenCookie(authResult.refreshToken()).toString());
        return headers;
    }
}
