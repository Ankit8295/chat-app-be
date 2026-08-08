package com.thechat.conversation.dto;

import java.time.Instant;
import java.util.UUID;

import com.thechat.conversation.ConversationParticipant;
import com.thechat.user.UserProfile;

public record ConversationParticipantResponse(
        UUID id,
        String name,
        String image,
        Instant joinedAt) {

    public static ConversationParticipantResponse from(
            ConversationParticipant participant,
            UserProfile profile) {
        return new ConversationParticipantResponse(
                participant.getUserId(),
                profile != null ? profile.name() : null,
                profile != null ? profile.image() : null,
                participant.getJoinedAt());
    }
}
