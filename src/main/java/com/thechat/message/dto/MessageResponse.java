package com.thechat.message.dto;

import java.time.Instant;
import java.util.UUID;

import com.thechat.message.Message;
import com.thechat.user.AppUser;

public record MessageResponse(
        UUID id,
        UUID conversationId,
        UUID senderId,
        String senderName,
        String senderImage,
        String content,
        Instant createdAt) {

    public static MessageResponse from(Message message) {
        AppUser sender = message.getSender();
        return new MessageResponse(
                message.getId(),
                message.getConversation().getId(),
                sender.getId(),
                sender.getName(),
                sender.getImage(),
                message.getContent(),
                message.getCreatedAt());
    }
}
