package com.thechat.auth;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.thechat.security.JwtProperties;
import com.thechat.security.JwtService;
import com.thechat.user.AppUser;
import com.thechat.user.UserNotFoundException;
import com.thechat.user.UserRepository;
import com.thechat.user.UserResponse;

@Service
public class RefreshTokenService {

    private static final int REFRESH_TOKEN_BYTES = 32;

    private final RefreshTokenStore refreshTokenStore;
    private final JwtService jwtService;
    private final JwtProperties jwtProperties;
    private final UserRepository userRepository;
    private final SecureRandom secureRandom = new SecureRandom();

    public RefreshTokenService(
            RefreshTokenStore refreshTokenStore,
            JwtService jwtService,
            JwtProperties jwtProperties,
            UserRepository userRepository) {
        this.refreshTokenStore = refreshTokenStore;
        this.jwtService = jwtService;
        this.jwtProperties = jwtProperties;
        this.userRepository = userRepository;
    }

    public String issue(UUID userId, String deviceId) {
        String rawToken = generateRawToken();
        String tokenHash = sha256Hex(rawToken);
        Instant now = Instant.now();
        Instant expiresAt = now.plus(jwtProperties.refreshTokenTtl());

        RefreshTokenSession session = new RefreshTokenSession(
                userId,
                deviceId,
                now,
                expiresAt,
                false);
        refreshTokenStore.save(tokenHash, session, jwtProperties.refreshTokenTtl());
        return rawToken;
    }

    public AuthResult rotate(String rawRefreshToken) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            throw new InvalidRefreshTokenException();
        }

        String tokenHash = sha256Hex(rawRefreshToken);
        RefreshTokenSession session = refreshTokenStore.getAndDelete(tokenHash)
                .orElseThrow(InvalidRefreshTokenException::new);

        if (session.revoked() || session.expiresAt().isBefore(Instant.now())) {
            throw new InvalidRefreshTokenException();
        }

        AppUser user = userRepository.findById(session.userId())
                .orElseThrow(() -> new UserNotFoundException(session.userId()));

        String accessToken = jwtService.createAccessToken(user);
        String newRefreshToken = issue(user.getId(), session.deviceId());

        return new AuthResult(
                accessToken,
                newRefreshToken,
                new com.thechat.auth.dto.AuthResponse(
                        jwtService.accessTokenExpiresInSeconds(),
                        UserResponse.from(user)));
    }

    public void revoke(String rawRefreshToken) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            return;
        }
        refreshTokenStore.delete(sha256Hex(rawRefreshToken));
    }

    private String generateRawToken() {
        byte[] bytes = new byte[REFRESH_TOKEN_BYTES];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    static String sha256Hex(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 not available", ex);
        }
    }
}
