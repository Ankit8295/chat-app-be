package com.thechat.conversation.dto;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.thechat.conversation.Conversation;
import com.thechat.conversation.ConversationType;
import com.thechat.user.FriendshipStatusResponse;
import com.thechat.user.UserProfile;

public record ConversationDetailResponse(
        UUID id,
        String type,
        String name,
        String image,
        String about,
        ConversationParticipantResponse friend,
        List<ConversationParticipantResponse> participants,
        UUID createdBy,
        Instant createdAt,
        Instant updatedAt,
        String blockStatus) {

    public static ConversationDetailResponse from(
            Conversation conversation,
            UUID currentUserId,
            Map<UUID, UserProfile> profileMap) {
        return from(conversation, currentUserId, profileMap, null);
    }

    public static ConversationDetailResponse from(
            Conversation conversation,
            UUID currentUserId,
            Map<UUID, UserProfile> profileMap,
            FriendshipStatusResponse friendshipStatus) {

        String derivedName = conversation.getName();
        String derivedImage = null;
        ConversationParticipantResponse friend = null;
        List<ConversationParticipantResponse> participants = List.of();
        String blockStatus = "none";

        if (conversation.getType() == ConversationType.DIRECT) {
            var other = conversation.getParticipants().stream()
                    .filter(p -> !p.getUserId().equals(currentUserId))
                    .findFirst()
                    .orElse(null);

            if (other != null) {
                UserProfile otherProfile = profileMap.get(other.getUserId());
                derivedName = otherProfile != null ? otherProfile.name() : null;
                derivedImage = otherProfile != null ? otherProfile.image() : null;
                friend = ConversationParticipantResponse.from(other, otherProfile);
            }

            if (friendshipStatus != null && "blocked".equals(friendshipStatus.status())) {
                if (friendshipStatus.blockedByMe()) {
                    blockStatus = "blocked_by_me";
                } else if (friendshipStatus.blockedByPeer()) {
                    blockStatus = "blocked_by_peer";
                }
            }
        } else {
            derivedImage = conversation.getImage();
            participants = conversation.getParticipants().stream()
                    .map(p -> ConversationParticipantResponse.from(p, profileMap.get(p.getUserId())))
                    .toList();
        }

        return new ConversationDetailResponse(
                conversation.getId(),
                conversation.getType().name().toLowerCase(),
                derivedName,
                derivedImage,
                conversation.getAbout(),
                friend,
                participants,
                conversation.getCreatedBy(),
                conversation.getCreatedAt(),
                conversation.getUpdatedAt(),
                blockStatus);
    }
}
