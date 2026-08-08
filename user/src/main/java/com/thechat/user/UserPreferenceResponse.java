package com.thechat.user;

import java.util.UUID;

public record UserPreferenceResponse(
        UUID userId,
        UUID lastConversationId) {

    public static UserPreferenceResponse from(UserPreference userPreference) {
        return new UserPreferenceResponse(
                userPreference.getUserId(),
                userPreference.getLastConversationId());
    }
}
