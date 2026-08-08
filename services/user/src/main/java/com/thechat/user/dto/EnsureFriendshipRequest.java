package com.thechat.user.dto;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;

public record EnsureFriendshipRequest(
        @NotNull UUID userId,
        @NotNull UUID friendUserId
) {
}
