package com.thechat.conversation.dto;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import com.thechat.conversation.Conversation;
import com.thechat.conversation.ConversationType;
import com.thechat.user.UserProfile;

public record ConversationResponse(
        UUID id,
        String type,
        String name,
        String image,
        UUID createdBy,
        Instant createdAt,
        Instant updatedAt) {

    /**
     * @param profileMap  userId → UserProfile fetched from User service via batch call
     */
    public static ConversationResponse from(
            Conversation conversation,
            UUID currentUserId,
            Map<UUID, UserProfile> profileMap) {

        String derivedName = conversation.getName();
        String derivedImage = null;

        if (conversation.getType() == ConversationType.DIRECT) {
            var other = conversation.getParticipants().stream()
                    .filter(p -> !p.getUserId().equals(currentUserId))
                    .findFirst()
                    .orElse(null);

            if (other != null) {
                UserProfile otherProfile = profileMap.get(other.getUserId());
                derivedName = otherProfile != null ? otherProfile.name() : null;
                derivedImage = otherProfile != null ? otherProfile.image() : null;
            }
        } else {
            derivedImage = conversation.getImage();
        }

        return new ConversationResponse(
                conversation.getId(),
                conversation.getType().name().toLowerCase(),
                derivedName,
                derivedImage,
                conversation.getCreatedBy(),
                conversation.getCreatedAt(),
                conversation.getUpdatedAt());
    }
}
