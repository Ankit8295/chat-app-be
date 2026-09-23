package com.thechat.friendship.dto;

public record FriendshipStatusResponse(
        String status,
        boolean blockedByMe,
        boolean blockedByPeer
) {
}
