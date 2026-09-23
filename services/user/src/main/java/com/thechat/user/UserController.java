package com.thechat.user;

import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.thechat.common.dto.PageResponse;
import com.thechat.friendship.dto.FriendResponse;
import com.thechat.user.dto.AvatarConfirmRequest;
import com.thechat.user.dto.AvatarPresignRequest;
import com.thechat.user.dto.CreateUserPreferenceRequest;
import com.thechat.user.dto.OwnIdentityKeyResponse;
import com.thechat.user.dto.ProfilePresignedUrlResponse;
import com.thechat.user.dto.PublicIdentityKeyResponse;
import com.thechat.user.dto.UpdateUserProfileRequest;
import com.thechat.user.dto.UpsertIdentityKeyRequest;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService userService;
    private final IdentityKeyService identityKeyService;

    public UserController(UserService userService, IdentityKeyService identityKeyService) {
        this.userService = userService;
        this.identityKeyService = identityKeyService;
    }

    @GetMapping
    public ResponseEntity<PageResponse<UserSearchResultResponse>> searchUsers(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "q", required = false) String q,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size) {
        UUID requesterId = UUID.fromString(jwt.getClaimAsString("userId"));
        String queryTerm = (search != null && !search.isBlank()) ? search : q;
        PageResponse<UserSearchResultResponse> users = userService.searchUsers(requesterId, queryTerm, page, size);
        return ResponseEntity.ok(users);
    }

    @GetMapping("/friends")
    public ResponseEntity<PageResponse<FriendResponse>> getFriends(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size) {
        UUID requesterId = UUID.fromString(jwt.getClaimAsString("userId"));
        PageResponse<FriendResponse> friends = userService.getFriends(requesterId, page, size);
        return ResponseEntity.ok(friends);
    }

    @DeleteMapping("/friends/{friendUserId}")
    public ResponseEntity<Void> removeFriend(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID friendUserId) {
        UUID requesterId = UUID.fromString(jwt.getClaimAsString("userId"));
        userService.removeFriend(requesterId, friendUserId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/friends/{userId}/block")
    public ResponseEntity<Void> blockUser(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID userId) {
        UUID requesterId = UUID.fromString(jwt.getClaimAsString("userId"));
        userService.blockUser(requesterId, userId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/friends/{userId}/block")
    public ResponseEntity<Void> unblockUser(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID userId) {
        UUID requesterId = UUID.fromString(jwt.getClaimAsString("userId"));
        userService.unblockUser(requesterId, userId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponse> getMe(@AuthenticationPrincipal Jwt jwt) {
        UUID requesterId = UUID.fromString(jwt.getClaimAsString("userId"));
        return ResponseEntity.ok(userService.getUserById(requesterId));
    }

    @PutMapping("/me")
    public ResponseEntity<UserResponse> updateMe(@AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody UpdateUserProfileRequest request) {
        UUID requesterId = UUID.fromString(jwt.getClaimAsString("userId"));
        return ResponseEntity.ok(userService.updateProfile(requesterId, request));
    }

    @PostMapping("/me/avatar/presign")
    public ResponseEntity<ProfilePresignedUrlResponse> presignAvatar(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody AvatarPresignRequest request) {
        UUID requesterId = UUID.fromString(jwt.getClaimAsString("userId"));
        return ResponseEntity.ok(userService.createAvatarPresign(requesterId, request));
    }

    @PostMapping("/me/avatar/confirm")
    public ResponseEntity<UserResponse> confirmAvatar(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody AvatarConfirmRequest request) {
        UUID requesterId = UUID.fromString(jwt.getClaimAsString("userId"));
        return ResponseEntity.ok(userService.confirmAvatarUpload(requesterId, request));
    }

    @DeleteMapping("/me/avatar")
    public ResponseEntity<UserResponse> removeAvatar(@AuthenticationPrincipal Jwt jwt) {
        UUID requesterId = UUID.fromString(jwt.getClaimAsString("userId"));
        return ResponseEntity.ok(userService.removeAvatar(requesterId));
    }

    @GetMapping("/me/preferences")
    public ResponseEntity<UserPreferenceResponse> getPreference(@AuthenticationPrincipal Jwt jwt) {
        UUID requesterId = UUID.fromString(jwt.getClaimAsString("userId"));
        return ResponseEntity.ok(userService.getUserPreference(requesterId));
    }

    @PostMapping("/me/preferences")
    public ResponseEntity<UserPreferenceResponse> setUserPreference(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CreateUserPreferenceRequest request) {
        UUID requesterId = UUID.fromString(jwt.getClaimAsString("userId"));
        UUID lastConversationId = request.lastConversationId();
        UserPreferenceResponse userPreference = userService.setUserPreference(requesterId, lastConversationId);
        return ResponseEntity.ok(userPreference);
    }

    @GetMapping("/me/crypto")
    public ResponseEntity<OwnIdentityKeyResponse> getMyCrypto(@AuthenticationPrincipal Jwt jwt) {
        UUID requesterId = UUID.fromString(jwt.getClaimAsString("userId"));
        return ResponseEntity.ok(identityKeyService.getOwn(requesterId));
    }

    @PutMapping("/me/crypto")
    public ResponseEntity<OwnIdentityKeyResponse> putMyCrypto(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody UpsertIdentityKeyRequest request) {
        UUID requesterId = UUID.fromString(jwt.getClaimAsString("userId"));
        return ResponseEntity.ok(identityKeyService.upsert(requesterId, request));
    }

    @GetMapping("/crypto")
    public ResponseEntity<List<PublicIdentityKeyResponse>> getPublicCrypto(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam("ids") List<UUID> ids) {
        return ResponseEntity.ok(identityKeyService.getPublicKeys(ids));
    }

    @GetMapping("/{userId}")
    public ResponseEntity<UserResponse> getUserById(@PathVariable UUID userId) {
        return ResponseEntity.ok(userService.getUserById(userId));
    }
}
