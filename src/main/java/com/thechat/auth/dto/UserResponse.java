package com.thechat.auth.dto;

import com.thechat.user.AppUser;
import java.util.UUID;

public record UserResponse(
        UUID id,
        String email,
        String name
) {
    public static UserResponse from(AppUser user) {
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getName()
        );
    }
}
