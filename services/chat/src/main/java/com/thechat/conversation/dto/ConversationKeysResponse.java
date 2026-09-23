package com.thechat.conversation.dto;

import java.util.List;

public record ConversationKeysResponse(
        List<ConversationKeyEnvelopeResponse> envelopes) {
}
