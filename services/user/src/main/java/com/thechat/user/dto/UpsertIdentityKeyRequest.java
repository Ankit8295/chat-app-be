package com.thechat.user.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpsertIdentityKeyRequest(
        @NotBlank String publicKey,
        @NotBlank String wrappedPrivateKey,
        @NotBlank String wrapNonce,
        @NotBlank String kdfSalt,
        @NotNull @Min(1) Integer kdfIterations,
        @NotBlank @Size(max = 64) String algorithm) {
}
