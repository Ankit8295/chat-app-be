package com.thechat.auth;

import java.util.Locale;
import java.util.UUID;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.thechat.auth.dto.AuthResponse;
import com.thechat.auth.dto.LoginRequest;
import com.thechat.auth.dto.RegisterRequest;
import com.thechat.security.JwtService;

/**
 * Phase 2: AuthService owns only credentials (email + passwordHash).
 *
 * Register saga:
 *   1. Persist credential locally (auth_db).
 *   2. Call User service to create profile (HTTP).
 *   Compensation: if step 2 fails, delete the credential and rethrow.
 *
 * KNOWN TRADE-OFF: if the JVM crashes between step 1 and step 2 the credential is orphaned.
 * Fix: add an outbox/saga cleanup job in a future phase.
 */
@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final PasswordEncoder passwordEncoder;
    private final CredentialRepository credentialRepository;
    private final UserProfileClient userProfileClient;

    public AuthService(
            AuthenticationManager authenticationManager,
            JwtService jwtService,
            RefreshTokenService refreshTokenService,
            PasswordEncoder passwordEncoder,
            CredentialRepository credentialRepository,
            UserProfileClient userProfileClient) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
        this.passwordEncoder = passwordEncoder;
        this.credentialRepository = credentialRepository;
        this.userProfileClient = userProfileClient;
    }

    /**
     * Register saga: credential → HTTP profile creation → compensation on failure.
     * Not @Transactional on purpose: the credential is committed before the HTTP call so that
     * compensation (deleteById) works correctly as a separate unit of work.
     */
    public AuthResult register(RegisterRequest request) {
        String email = normalizeEmail(request.email());
        if (credentialRepository.existsByEmail(email)) {
            throw new EmailAlreadyExistsException(email);
        }

        UUID userId = UUID.randomUUID();
        UserCredential credential = new UserCredential(
                userId,
                email,
                passwordEncoder.encode(request.password()));
        credentialRepository.save(credential);

        try {
            userProfileClient.createProfile(userId, email, request.name().trim());
        } catch (Exception e) {
            credentialRepository.deleteById(userId);
            throw new RegistrationFailedException(
                    "Registration failed: user profile could not be created. Credential rolled back.", e);
        }

        return buildAuthResponse(credential, null);
    }

    public AuthResult login(LoginRequest request) {
        String email = normalizeEmail(request.email());
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(email, request.password()));

        UserCredential credential = credentialRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("Authenticated credential was not found"));

        return buildAuthResponse(credential, null);
    }

    public AuthResult refresh(String rawRefreshToken) {
        return refreshTokenService.rotate(rawRefreshToken);
    }

    public void logout(String rawRefreshToken) {
        refreshTokenService.revoke(rawRefreshToken);
    }

    private AuthResult buildAuthResponse(UserCredential credential, String deviceId) {
        String accessToken = jwtService.createAccessToken(credential);
        String refreshToken = refreshTokenService.issue(credential.getId(), deviceId);
        AuthResponse response = new AuthResponse(jwtService.accessTokenExpiresInSeconds());
        return new AuthResult(accessToken, refreshToken, response);
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
