package com.thechat.conversation;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.FORBIDDEN)
public class ForbiddenUserException extends RuntimeException {

    public ForbiddenUserException() {
        super("You are not allowed to update this conversation");
    }
}