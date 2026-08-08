package com.thechat.auth;

/**
 * Thrown when the register saga fails at the "create user profile" step.
 * The credential that was already persisted is compensated (deleted) before this is thrown.
 */
public class RegistrationFailedException extends RuntimeException {

    public RegistrationFailedException(String message, Throwable cause) {
        super(message, cause);
    }
}
