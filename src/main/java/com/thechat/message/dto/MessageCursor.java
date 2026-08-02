package com.thechat.message.dto;

import java.time.Instant;
import java.util.UUID;

public record MessageCursor(
        Instant createdAt,
        UUID id) {

    public String encode() {
        return createdAt + "|" + id;
    }

    public static MessageCursor decode(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String[] parts = raw.split("\\|", 2);
        if (parts.length != 2) {
            throw new IllegalArgumentException("Invalid cursor format");
        }
        try {
            return new MessageCursor(Instant.parse(parts[0]), UUID.fromString(parts[1]));
        } catch (RuntimeException ex) {
            throw new IllegalArgumentException("Invalid cursor format", ex);
        }
    }

    public static MessageCursor from(Instant createdAt, UUID id) {
        return new MessageCursor(createdAt, id);
    }
}
