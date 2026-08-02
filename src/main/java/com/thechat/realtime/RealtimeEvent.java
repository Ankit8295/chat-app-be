package com.thechat.realtime;

import java.util.List;
import java.util.UUID;

import tools.jackson.databind.JsonNode;

public record RealtimeEvent(
        UUID eventId,
        String type,
        List<UUID> targetUserIds,
        JsonNode envelope,
        String originInstanceId) {
}
