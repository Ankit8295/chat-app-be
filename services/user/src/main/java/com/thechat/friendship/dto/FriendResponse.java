package com.thechat.friendship.dto;

import java.time.Instant;
import java.util.UUID;

import com.thechat.friendship.Friendship;

public record FriendResponse(
    UUID userId,
    String name,
    String email,
    String profileImage,
    String friendshipStatus,
    Instant createdAt,
    Instant updatedAt
) {
    public static FriendResponse from(Friendship friendship, String profileImageUrl) {
        return new FriendResponse(
            friendship.getFriendUser().getId(),
            friendship.getFriendUser().getName(),
            friendship.getFriendUser().getEmail(),
            profileImageUrl,
            friendship.getStatus().name().toLowerCase(),
            friendship.getCreatedAt(),
            friendship.getUpdatedAt()
        );
    }
}
