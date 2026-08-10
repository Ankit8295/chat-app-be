package com.thechat.user;

import java.util.UUID;

public record UserResponse(
        UUID id,
        String email,
        String name,
        String image,
        String about) {
}
