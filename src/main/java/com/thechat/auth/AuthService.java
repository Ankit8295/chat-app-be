package com.thechat.auth;

import java.util.Locale;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.thechat.auth.dto.AuthResponse;
import com.thechat.auth.dto.LoginRequest;
import com.thechat.auth.dto.RegisterRequest;
import com.thechat.security.JwtService;
import com.thechat.user.AppUser;
import com.thechat.user.UserRepository;
import com.thechat.user.UserResponse;

@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;

    public AuthService(
            AuthenticationManager authenticationManager,
            JwtService jwtService,
            RefreshTokenService refreshTokenService,
            PasswordEncoder passwordEncoder,
            UserRepository userRepository) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
        this.passwordEncoder = passwordEncoder;
        this.userRepository = userRepository;
    }

    @Transactional
    public AuthResult register(RegisterRequest request) {
        String email = normalizeEmail(request.email());
        if (userRepository.existsByEmail(email)) {
            throw new EmailAlreadyExistsException(email);
        }

        AppUser user = new AppUser(
                email,
                request.name().trim(),
                passwordEncoder.encode(request.password()));
        AppUser savedUser = userRepository.save(user);

        return buildAuthResponse(savedUser, null);
    }

    @Transactional(readOnly = true)
    public AuthResult login(LoginRequest request) {
        String email = normalizeEmail(request.email());
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(email, request.password()));

        AppUser user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("Authenticated user was not found"));

        return buildAuthResponse(user, null);
    }

    @Transactional(readOnly = true)
    public AuthResult refresh(String rawRefreshToken) {
        return refreshTokenService.rotate(rawRefreshToken);
    }

    public void logout(String rawRefreshToken) {
        refreshTokenService.revoke(rawRefreshToken);
    }

    private AuthResult buildAuthResponse(AppUser user, String deviceId) {
        String accessToken = jwtService.createAccessToken(user);
        String refreshToken = refreshTokenService.issue(user.getId(), deviceId);
        AuthResponse response = new AuthResponse(
                jwtService.accessTokenExpiresInSeconds(),
                UserResponse.from(user));

        return new AuthResult(accessToken, refreshToken, response);
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
