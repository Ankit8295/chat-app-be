package com.thechat.urlshortener.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ShortenUrlRequest(
        @NotBlank(message = "longUrl is required")
        @Size(max = 5000, message = "longUrl must be at most 5000 characters")
        String longUrl
) {
}
