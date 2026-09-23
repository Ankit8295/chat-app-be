package com.thechat.conversation.dto;

import java.util.UUID;

import com.thechat.conversation.ConversationKeyEnvelope;

public record ConversationKeyEnvelopeResponse(
        UUID userId,
        int keyVersion,
        String wrappedKey,
        String wrapNonce,
        String ephPublicKey) {

    public static ConversationKeyEnvelopeResponse from(ConversationKeyEnvelope envelope) {
        return new ConversationKeyEnvelopeResponse(
                envelope.getUserId(),
                envelope.getKeyVersion(),
                envelope.getWrappedKey(),
                envelope.getWrapNonce(),
                envelope.getEphPublicKey());
    }
}
