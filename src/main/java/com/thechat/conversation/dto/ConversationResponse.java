package com.thechat.conversation.dto;

import java.time.Instant;
import java.util.UUID;

import com.thechat.conversation.Conversation;
import com.thechat.conversation.ConversationType;
import com.thechat.user.AppUser;

public record ConversationResponse(
    UUID id,
    String type,
    String name,
    String image,
    UUID friendId,
    Instant createdAt,
    Instant updatedAt
) {
    public static ConversationResponse from(Conversation conversation, UUID currentUserId) {
        String derivedName = conversation.getName();
        String derivedImage = conversation.getImage();
        UUID friendId = null;

        if (conversation.getType() == ConversationType.DIRECT) {
            var otherParticipantOpt = conversation.getParticipants().stream()
                .filter(p -> !p.getUser().getId().equals(currentUserId))
                .findFirst();

            if (otherParticipantOpt.isPresent()) {
                AppUser otherUser = otherParticipantOpt.get().getUser();
                derivedName = otherUser.getName();
                friendId = otherUser.getId();
            }
        }

        return new ConversationResponse(
            conversation.getId(),
            conversation.getType().name().toLowerCase(),
            derivedName,
            derivedImage,
            friendId,
            conversation.getCreatedAt(),
            conversation.getUpdatedAt()
        );
    }
}
