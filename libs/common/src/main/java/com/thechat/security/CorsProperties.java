package com.thechat.security;

import java.util.Arrays;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.cors")
public record CorsProperties(List<String> allowedOrigins) {

    public CorsProperties {
        // Env often injects a single comma-separated string via ${CORS_ALLOWED_ORIGINS}.
        if (allowedOrigins != null && allowedOrigins.size() == 1 && allowedOrigins.getFirst().contains(",")) {
            allowedOrigins = Arrays.stream(allowedOrigins.getFirst().split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .toList();
        }
    }
}
