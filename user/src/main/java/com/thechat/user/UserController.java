package com.thechat.user;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.thechat.common.dto.PageResponse;
import com.thechat.friendship.dto.FriendResponse;
import com.thechat.user.dto.CreateUserPreferenceRequest;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
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

    @GetMapping("/me")
    public ResponseEntity<UserResponse> getMe(@AuthenticationPrincipal Jwt jwt) {
        UUID requesterId = UUID.fromString(jwt.getClaimAsString("userId"));
        return ResponseEntity.ok(userService.getUserById(requesterId));
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

    @GetMapping("/{userId}")
    public ResponseEntity<UserResponse> getUserById(@PathVariable UUID userId) {
        return ResponseEntity.ok(userService.getUserById(userId));
    }
}
