package com.thechat.ws.dto;

import tools.jackson.databind.JsonNode;

public record WsEnvelope(
        String type,
        JsonNode payload) {
}
