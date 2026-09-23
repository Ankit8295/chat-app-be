package com.thechat.user;

public record FriendshipStatusResponse(
        String status,
        boolean blockedByMe,
        boolean blockedByPeer
) {
}
