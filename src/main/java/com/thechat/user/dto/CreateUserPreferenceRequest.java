package com.thechat.user.dto;

import java.util.UUID;

public record CreateUserPreferenceRequest(
        UUID lastConversationId) {
}
