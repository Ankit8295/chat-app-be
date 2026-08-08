package com.thechat.user;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.thechat.user.dto.CreateUserProfileRequest;
import com.thechat.user.dto.EnsureFriendshipRequest;

import jakarta.validation.Valid;

/**
 * Internal service-to-service API — not publicly routable (blocked at nginx).
 *
 * Called by:
 *   Auth service   — POST /internal/users (register saga step 2)
 *                    DELETE /internal/users/{userId} (register compensation)
 *   Chat service   — GET /internal/users?ids=... (batch profile fetch, avoids network N+1)
 *                    POST /internal/friendships/ensure (direct conversation creates friendship)
 *
 * Phase 5 will add mTLS / service token enforcement at the gateway.
 */
@RestController
@RequestMapping("/internal")
public class InternalUserController {

    private final UserService userService;

    public InternalUserController(UserService userService) {
        this.userService = userService;
    }

    // ── Auth service calls ──────────────────────────────────────────────────

    @PostMapping("/users")
    public ResponseEntity<Void> createProfile(@Valid @RequestBody CreateUserProfileRequest request) {
        userService.createProfile(request.userId(), request.email(), request.name());
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @DeleteMapping("/users/{userId}")
    public ResponseEntity<Void> deleteProfile(@PathVariable UUID userId) {
        userService.deleteProfile(userId);
        return ResponseEntity.noContent().build();
    }

    // ── Chat service calls ──────────────────────────────────────────────────

    /**
     * Batch profile fetch — avoids N+1 HTTP calls when building conversation/message responses.
     * Example: GET /internal/users?ids=uuid1,uuid2,uuid3
     */
    @GetMapping("/users")
    public ResponseEntity<List<UserResponse>> getProfilesByIds(
            @RequestParam("ids") List<UUID> ids) {
        return ResponseEntity.ok(userService.getProfilesByIds(ids));
    }

    /**
     * Ensures a bidirectional friendship between two users.
     * Called by Chat when a DIRECT conversation is opened for the first time.
     */
    @PostMapping("/friendships/ensure")
    public ResponseEntity<Void> ensureFriendship(@Valid @RequestBody EnsureFriendshipRequest request) {
        userService.ensureFriendship(request.userId(), request.friendUserId());
        return ResponseEntity.ok().build();
    }
}
