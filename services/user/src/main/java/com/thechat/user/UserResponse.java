package com.thechat.user;

import java.util.UUID;

public record UserResponse(
        UUID id,
        String email,
        String name,
        String image,
        String about) {
    public static UserResponse from(AppUser user) {
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getName(),
                user.getImage(),
                user.getAbout());
    }
}
