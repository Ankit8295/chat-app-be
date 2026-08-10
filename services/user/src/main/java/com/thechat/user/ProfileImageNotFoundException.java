package com.thechat.user;

public class ProfileImageNotFoundException extends RuntimeException {

    public ProfileImageNotFoundException(java.util.UUID mediaId) {
        super("Profile image not found: " + mediaId);
    }
}
