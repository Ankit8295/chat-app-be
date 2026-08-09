package com.thechat.user.dto;

import java.util.UUID;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateUserProfileRequest(
                @NotNull UUID userId,
                @NotBlank @Email String email,
                @NotBlank @Size(min = 2, max = 80) String name) {
}
