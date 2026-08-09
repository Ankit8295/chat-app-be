package com.thechat.conversation.dto;

import jakarta.validation.constraints.Size;

public record UpdateGroupConversationRequest(
                @Size(min = 2, max = 50) String name,
                @Size(max = 200) String about) {
}
