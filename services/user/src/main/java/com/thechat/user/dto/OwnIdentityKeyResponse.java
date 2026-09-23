package com.thechat.user.dto;

import java.util.UUID;

import com.thechat.user.UserIdentityKey;

public record OwnIdentityKeyResponse(
        UUID userId,
        String publicKey,
        String wrappedPrivateKey,
        String wrapNonce,
        String kdfSalt,
        int kdfIterations,
        String algorithm) {

    public static OwnIdentityKeyResponse from(UserIdentityKey key) {
        return new OwnIdentityKeyResponse(
                key.getUserId(),
                key.getPublicKey(),
                key.getWrappedPrivateKey(),
                key.getWrapNonce(),
                key.getKdfSalt(),
                key.getKdfIterations(),
                key.getAlgorithm());
    }
}
