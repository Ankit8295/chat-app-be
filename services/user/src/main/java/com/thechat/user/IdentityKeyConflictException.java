package com.thechat.user;

public class IdentityKeyConflictException extends RuntimeException {

    public IdentityKeyConflictException() {
        super("Identity key already exists");
    }
}
