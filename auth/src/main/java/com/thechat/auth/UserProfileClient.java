package com.thechat.auth;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * HTTP client from Auth service → User service.
 *
 * Calling pattern (register saga):
 *   Step 1 — Auth persists credential (local DB transaction).
 *   Step 2 — Auth calls User to create profile (this client).
 *   Compensation — if step 2 fails, Auth deletes the credential (deleteProfile never called on user
 *                  here because the User never created the profile; Auth just deletes its own record).
 *
 * For now there is no mTLS or service token; that is added in Phase 5 (API gateway / mTLS).
 * The /internal/** paths are blocked at the nginx level so they are not publicly accessible.
 */
@Component
public class UserProfileClient {

    private static final Logger log = LoggerFactory.getLogger(UserProfileClient.class);

    private final RestClient restClient;

    public UserProfileClient(
            RestClient.Builder restClientBuilder,
            @Value("${app.services.user.base-url}") String userServiceBaseUrl) {
        this.restClient = restClientBuilder.baseUrl(userServiceBaseUrl).build();
    }

    /**
     * Creates a user profile in the User service.
     * Called after the credential has been persisted in the Auth DB.
     */
    public void createProfile(UUID userId, String email, String name) {
        log.debug("Creating user profile for userId={}", userId);
        restClient.post()
                .uri("/internal/users")
                .body(new CreateProfileRequest(userId, email, name))
                .retrieve()
                .toBodilessEntity();
    }

    /**
     * Deletes a user profile — compensation step.
     * Called when Auth created a credential but profile creation in User service failed on a
     * previous registration attempt.
     */
    public void deleteProfile(UUID userId) {
        log.warn("Compensation: deleting orphaned profile for userId={}", userId);
        try {
            restClient.delete()
                    .uri("/internal/users/{userId}", userId)
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception e) {
            log.error("Compensation deleteProfile failed for userId={}. Manual cleanup may be required.", userId, e);
        }
    }

    record CreateProfileRequest(UUID userId, String email, String name) {
    }
}
