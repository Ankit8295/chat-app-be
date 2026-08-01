package com.thechat.conversation.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.thechat.conversation.Conversation;
import com.thechat.conversation.ConversationParticipant;
import com.thechat.conversation.ConversationType;
import com.thechat.user.AppUser;

public record ConversationDetailResponse(
        UUID id,
        String type,
        String name,
        String image,
        ConversationParticipantResponse friend,
        List<ConversationParticipantResponse> participants,
        UUID createdBy,
        Instant createdAt,
        Instant updatedAt) {

    public static ConversationDetailResponse from(Conversation conversation, UUID currentUserId) {
        String derivedName = conversation.getName();
        String derivedImage = null;
        ConversationParticipantResponse friend = null;
        List<ConversationParticipantResponse> participants = List.of();

        if (conversation.getType() == ConversationType.DIRECT) {
            ConversationParticipant other = conversation.getParticipants().stream()
                    .filter(p -> !p.getUser().getId().equals(currentUserId))
                    .findFirst()
                    .orElse(null);

            if (other != null) {
                AppUser otherUser = other.getUser();
                derivedName = otherUser.getName();
                derivedImage = otherUser.getImage();
                friend = ConversationParticipantResponse.from(other);
            }
        } else {
            derivedImage = conversation.getImage();
            participants = conversation.getParticipants().stream()
                    .map(ConversationParticipantResponse::from)
                    .toList();
        }

        return new ConversationDetailResponse(
                conversation.getId(),
                conversation.getType().name().toLowerCase(),
                derivedName,
                derivedImage,
                friend,
                participants,
                conversation.getCreatedBy(),
                conversation.getCreatedAt(),
                conversation.getUpdatedAt());
    }
}
