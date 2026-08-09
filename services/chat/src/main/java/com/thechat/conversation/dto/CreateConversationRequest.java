package com.thechat.conversation.dto;

import java.util.List;
import java.util.UUID;

import com.thechat.conversation.ConversationType;

import jakarta.validation.constraints.NotNull;

@ValidCreateConversation
public record CreateConversationRequest(
                UUID userId,
                @NotNull ConversationType type,
                String name,
                String about,
                String image,
                List<UUID> participants) {
}
