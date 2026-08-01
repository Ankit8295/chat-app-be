package com.thechat.conversation.dto;

import java.time.Instant;
import java.util.UUID;

import com.thechat.conversation.ConversationParticipant;
import com.thechat.user.AppUser;

public record ConversationParticipantResponse(
        UUID id,
        String name,
        String image,
        Instant joinedAt) {

    public static ConversationParticipantResponse from(ConversationParticipant participant) {
        AppUser user = participant.getUser();
        return new ConversationParticipantResponse(
                user.getId(),
                user.getName(),
                user.getImage(),
                participant.getJoinedAt());
    }
}
