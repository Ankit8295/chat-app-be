package com.thechat.user.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AvatarPresignRequest(
        @NotBlank String contentType,
        @NotBlank @Size(max = 255) String fileName,
        @NotNull @Min(1) Long sizeBytes) {
}
