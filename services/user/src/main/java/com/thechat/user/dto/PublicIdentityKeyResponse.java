package com.thechat.user.dto;

import java.util.UUID;

import com.thechat.user.UserIdentityKey;

public record PublicIdentityKeyResponse(
        UUID userId,
        String publicKey) {

    public static PublicIdentityKeyResponse from(UserIdentityKey key) {
        return new PublicIdentityKeyResponse(key.getUserId(), key.getPublicKey());
    }
}
