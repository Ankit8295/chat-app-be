package com.thechat.user;

import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.thechat.friendship.dto.FriendResponse;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public ResponseEntity<List<UserSearchResultResponse>> searchUsers(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "q", required = false) String q) {
        UUID requesterId = UUID.fromString(jwt.getClaimAsString("userId"));
        String queryTerm = (search != null && !search.isBlank()) ? search : q;
        List<UserSearchResultResponse> users = userService.searchUsers(requesterId, queryTerm);
        return ResponseEntity.ok(users);
    }

    @GetMapping("/friends")
    public ResponseEntity<List<FriendResponse>> getFriends(@AuthenticationPrincipal Jwt jwt) {
        UUID requesterId = UUID.fromString(jwt.getClaimAsString("userId"));
        List<FriendResponse> friends = userService.getFriends(requesterId);
        return ResponseEntity.ok(friends);
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponse> getMe(@AuthenticationPrincipal Jwt jwt) {
        UUID requesterId = UUID.fromString(jwt.getClaimAsString("userId"));
        return ResponseEntity.ok(userService.getUserById(requesterId));
    }

    @GetMapping("/{userId}")
    public ResponseEntity<UserResponse> getUserById(@PathVariable UUID userId) {
        return ResponseEntity.ok(userService.getUserById(userId));
    }
}
