package com.thechat.user;

public class IdentityKeyNotFoundException extends RuntimeException {

    public IdentityKeyNotFoundException() {
        super("Identity key not found");
    }
}
