package com.thechat.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.thechat.security.JwtProperties;
import com.thechat.security.JwtService;
import com.thechat.user.AppUser;
import com.thechat.user.UserRepository;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    @Mock
    private RefreshTokenStore refreshTokenStore;

    @Mock
    private JwtService jwtService;

    @Mock
    private UserRepository userRepository;

    private RefreshTokenService refreshTokenService;

    private final JwtProperties jwtProperties = new JwtProperties(
            "the-chat-api",
            "replace-this-dummy-secret-with-at-least-32-characters",
            Duration.ofHours(1),
            Duration.ofDays(30));

    @BeforeEach
    void setUp() {
        refreshTokenService = new RefreshTokenService(
                refreshTokenStore,
                jwtService,
                jwtProperties,
                userRepository);
    }

    @Test
    void issueStoresHashedTokenNotPlaintext() {
        UUID userId = UUID.randomUUID();

        String raw = refreshTokenService.issue(userId, "device-1");

        assertThat(raw).isNotBlank();
        ArgumentCaptor<String> hashCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<RefreshTokenSession> sessionCaptor = ArgumentCaptor.forClass(RefreshTokenSession.class);
        verify(refreshTokenStore).save(hashCaptor.capture(), sessionCaptor.capture(), eq(Duration.ofDays(30)));

        assertThat(hashCaptor.getValue()).isEqualTo(RefreshTokenService.sha256Hex(raw));
        assertThat(hashCaptor.getValue()).isNotEqualTo(raw);
        assertThat(sessionCaptor.getValue().userId()).isEqualTo(userId);
        assertThat(sessionCaptor.getValue().deviceId()).isEqualTo("device-1");
        assertThat(sessionCaptor.getValue().revoked()).isFalse();
    }

    @Test
    void rotateReturnsNewAccessAndRefreshTokens() {
        UUID userId = UUID.randomUUID();
        String oldRaw = "old-refresh-token-value";
        String oldHash = RefreshTokenService.sha256Hex(oldRaw);
        Instant now = Instant.now();
        RefreshTokenSession existing = new RefreshTokenSession(
                userId,
                "device-1",
                now.minusSeconds(60),
                now.plus(Duration.ofDays(29)),
                false);

        AppUser user = org.mockito.Mockito.mock(AppUser.class);
        when(user.getId()).thenReturn(userId);
        when(user.getEmail()).thenReturn("user@example.com");
        when(user.getName()).thenReturn("User");
        when(user.getImage()).thenReturn(null);
        when(refreshTokenStore.getAndDelete(oldHash)).thenReturn(Optional.of(existing));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(jwtService.createAccessToken(user)).thenReturn("new-access-token");
        when(jwtService.accessTokenExpiresInSeconds()).thenReturn(3600L);

        AuthResult result = refreshTokenService.rotate(oldRaw);

        assertThat(result.accessToken()).isEqualTo("new-access-token");
        assertThat(result.refreshToken()).isNotBlank().isNotEqualTo(oldRaw);
        assertThat(result.response().expiresInSeconds()).isEqualTo(3600L);
        verify(refreshTokenStore).save(any(), any(), eq(Duration.ofDays(30)));
    }

    @Test
    void rotateRejectsMissingToken() {
        when(refreshTokenStore.getAndDelete(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> refreshTokenService.rotate("missing"))
                .isInstanceOf(InvalidRefreshTokenException.class);
    }

    @Test
    void rotateRejectsExpiredToken() {
        UUID userId = UUID.randomUUID();
        String raw = "expired-token";
        String hash = RefreshTokenService.sha256Hex(raw);
        Instant now = Instant.now();
        RefreshTokenSession expired = new RefreshTokenSession(
                userId,
                null,
                now.minus(Duration.ofDays(40)),
                now.minus(Duration.ofDays(1)),
                false);
        when(refreshTokenStore.getAndDelete(hash)).thenReturn(Optional.of(expired));

        assertThatThrownBy(() -> refreshTokenService.rotate(raw))
                .isInstanceOf(InvalidRefreshTokenException.class);
        verify(userRepository, never()).findById(any());
    }

    @Test
    void rotateRejectsRevokedToken() {
        UUID userId = UUID.randomUUID();
        String raw = "revoked-token";
        String hash = RefreshTokenService.sha256Hex(raw);
        Instant now = Instant.now();
        RefreshTokenSession revoked = new RefreshTokenSession(
                userId,
                null,
                now,
                now.plus(Duration.ofDays(1)),
                true);
        when(refreshTokenStore.getAndDelete(hash)).thenReturn(Optional.of(revoked));

        assertThatThrownBy(() -> refreshTokenService.rotate(raw))
                .isInstanceOf(InvalidRefreshTokenException.class);
    }

    @Test
    void revokeDeletesHashedKey() {
        String raw = "to-revoke";
        refreshTokenService.revoke(raw);
        verify(refreshTokenStore).delete(RefreshTokenService.sha256Hex(raw));
    }

    @Test
    void revokeIgnoresBlankToken() {
        refreshTokenService.revoke("  ");
        verify(refreshTokenStore, never()).delete(any());
    }
}
