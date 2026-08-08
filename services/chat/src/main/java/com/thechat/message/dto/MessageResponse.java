package com.thechat.message.dto;

import java.time.Instant;
import java.util.UUID;

import com.thechat.message.Message;
import com.thechat.user.UserProfile;

public record MessageResponse(
        UUID id,
        UUID conversationId,
        UUID senderId,
        String senderName,
        String senderImage,
        String content,
        Instant createdAt) {

    /**
     * @param senderProfile profile fetched from User service; null = graceful degradation
     */
    public static MessageResponse from(Message message, UserProfile senderProfile) {
        return new MessageResponse(
                message.getId(),
                message.getConversation().getId(),
                message.getSenderId(),
                senderProfile != null ? senderProfile.name() : null,
                senderProfile != null ? senderProfile.image() : null,
                message.getContent(),
                message.getCreatedAt());
    }
}
