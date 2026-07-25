package com.thechat.conversation.dto;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;

public record CreateConversationRequest(
    @NotNull(message = "userId is required")
    UUID userId
) {
}
