package com.thechat.urlshortener.dto;

import java.time.Instant;
import java.util.UUID;

public record UrlResponse(
        UUID id,
        String longUrl,
        String shortCode,
        String shortUrl,
        Instant createdAt
) {
}
