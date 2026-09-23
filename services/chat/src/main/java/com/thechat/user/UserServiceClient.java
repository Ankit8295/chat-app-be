package com.thechat.user;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.thechat.security.ServiceAuthRequestInterceptor;

/**
 * HTTP client from Chat service → User service.
 *
 * Key pattern: batch fetch by IDs to avoid the "network N+1" problem.
 *   Bad:  for each participant → GET /internal/users/{id}  (N round-trips)
 *   Good: GET /internal/users?ids=id1,id2,id3             (1 round-trip)
 *
 * Phase 5: every call carries a short-lived service token (ServiceAuthRequestInterceptor) so
 * User's /internal/** routes can verify the caller's identity instead of trusting network position.
 * Phase 6 will add a short-lived cache (Cache-Aside) on top of this client.
 */
@Component
public class UserServiceClient {

    private static final Logger log = LoggerFactory.getLogger(UserServiceClient.class);

    private final RestClient restClient;

    public UserServiceClient(
            RestClient.Builder restClientBuilder,
            @Value("${app.services.user.base-url}") String userServiceBaseUrl,
            ServiceAuthRequestInterceptor serviceAuthRequestInterceptor) {
        this.restClient = restClientBuilder
                .baseUrl(userServiceBaseUrl)
                .requestInterceptor(serviceAuthRequestInterceptor)
                .build();
    }

    /**
     * Batch-fetch profiles for the given IDs. Returns a map keyed by userId.
     * Missing users are silently omitted (graceful degradation — their name/image will be null).
     */
    public Map<UUID, UserProfile> batchGetByIds(List<UUID> ids) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyMap();
        }

        try {
            String idsParam = ids.stream()
                    .map(UUID::toString)
                    .collect(Collectors.joining(","));

            List<UserProfile> profiles = restClient.get()
                    .uri("/internal/users?ids={ids}", idsParam)
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<UserProfile>>() {
                    });

            if (profiles == null) {
                return Collections.emptyMap();
            }

            return profiles.stream()
                    .collect(Collectors.toMap(UserProfile::id, Function.identity()));
        } catch (Exception e) {
            log.error("Failed to batch-fetch user profiles for ids={}", ids, e);
            return Collections.emptyMap();
        }
    }

    /**
     * Ensure a bidirectional friendship exists between two users.
     * Called by Chat when a DIRECT conversation is opened for the first time.
     */
    public void ensureFriendship(UUID userId, UUID friendUserId) {
        try {
            restClient.post()
                    .uri("/internal/friendships/ensure")
                    .body(new EnsureFriendshipRequest(userId, friendUserId))
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception e) {
            log.warn("Failed to ensure friendship between {} and {}: {}", userId, friendUserId, e.getMessage());
        }
    }

    /**
     * Returns friendship/block status between two users.
     * @param failClosed when true (send path), treat errors as blocked; when false (detail), treat as none.
     */
    public FriendshipStatusResponse getFriendshipStatus(UUID userId, UUID otherUserId, boolean failClosed) {
        try {
            FriendshipStatusResponse status = restClient.get()
                    .uri("/internal/friendships/status?userId={userId}&otherUserId={otherUserId}",
                            userId, otherUserId)
                    .retrieve()
                    .body(FriendshipStatusResponse.class);
            if (status == null) {
                return failClosed
                        ? new FriendshipStatusResponse("blocked", true, true)
                        : new FriendshipStatusResponse("none", false, false);
            }
            return status;
        } catch (Exception e) {
            log.error("Failed to fetch friendship status between {} and {}", userId, otherUserId, e);
            return failClosed
                    ? new FriendshipStatusResponse("blocked", true, true)
                    : new FriendshipStatusResponse("none", false, false);
        }
    }

    public FriendshipStatusResponse getFriendshipStatus(UUID userId, UUID otherUserId) {
        return getFriendshipStatus(userId, otherUserId, true);
    }

    record EnsureFriendshipRequest(UUID userId, UUID friendUserId) {
    }
}
