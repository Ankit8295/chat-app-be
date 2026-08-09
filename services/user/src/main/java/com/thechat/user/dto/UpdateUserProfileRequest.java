package com.thechat.user.dto;

import jakarta.validation.constraints.Size;

public record UpdateUserProfileRequest(
        @Size(min = 1, max = 80) String name,
        @Size(max = 160) String about,
        String image) {
}
