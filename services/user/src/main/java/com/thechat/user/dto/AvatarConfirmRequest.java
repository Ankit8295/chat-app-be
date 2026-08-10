package com.thechat.user.dto;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;

public record AvatarConfirmRequest(
        @NotNull UUID mediaId) {
}
