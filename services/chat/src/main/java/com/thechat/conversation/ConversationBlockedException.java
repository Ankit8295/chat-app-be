package com.thechat.conversation;

public class ConversationBlockedException extends RuntimeException {

    public ConversationBlockedException() {
        super("Conversation is blocked");
    }
}
