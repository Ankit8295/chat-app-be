package com.thechat.ws.dto;

import java.util.UUID;

public record WsErrorPayload(
        String code,
        String message,
        UUID conversationId) {
}
