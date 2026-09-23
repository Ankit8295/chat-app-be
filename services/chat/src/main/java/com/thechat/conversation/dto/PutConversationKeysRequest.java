package com.thechat.conversation.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

public record PutConversationKeysRequest(
        @NotNull @Min(1) Integer keyVersion,
        @NotEmpty @Valid List<KeyEnvelopeRequest> envelopes) {
}
