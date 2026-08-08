package com.thechat.ws.dto;

import java.util.List;
import java.util.UUID;

public record SendMessagePayload(
                UUID conversationId,
                String content,
                List<String> attachmentIds) {
}
