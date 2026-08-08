package com.thechat.user;

import java.util.UUID;

/**
 * Chat service's view of a user profile — an anti-corruption layer DTO.
 * Chat never imports from the User module directly; it receives this via HTTP.
 */
public record UserProfile(UUID id, String name, String image) {
}
