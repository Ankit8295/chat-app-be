package com.thechat.user;

import java.util.UUID;

public record UserSearchResultResponse(
    UUID id,
    String email,
    String name,
    String img,
    String friendshipStatus
) {
    public static UserSearchResultResponse of(AppUser user, String friendshipStatus) {
        return new UserSearchResultResponse(
            user.getId(),
            user.getEmail(),
            user.getName(),
            user.getImage(),
            friendshipStatus
        );
    }
}
