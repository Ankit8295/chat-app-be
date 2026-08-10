package com.thechat.object_storage;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Validated
@ConfigurationProperties(prefix = "app.r2")
public record ObjectStorageProperties(
                @NotBlank String endpoint,
                @NotBlank String accessKeyId,
                @NotBlank String secretAccessKey,
                @NotBlank String bucket,
                @NotBlank String publicBaseUrl,
                @NotNull Duration putUrlTtl,
                @NotNull Duration getUrlTtl) {
}
