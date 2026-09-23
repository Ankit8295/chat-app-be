package com.thechat.conversation;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

public class ConversationKeyEnvelopeId implements Serializable {

    private UUID conversationId;
    private UUID userId;
    private int keyVersion;

    public ConversationKeyEnvelopeId() {
    }

    public ConversationKeyEnvelopeId(UUID conversationId, UUID userId, int keyVersion) {
        this.conversationId = conversationId;
        this.userId = userId;
        this.keyVersion = keyVersion;
    }

    public UUID getConversationId() {
        return conversationId;
    }

    public void setConversationId(UUID conversationId) {
        this.conversationId = conversationId;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public int getKeyVersion() {
        return keyVersion;
    }

    public void setKeyVersion(int keyVersion) {
        this.keyVersion = keyVersion;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ConversationKeyEnvelopeId other)) {
            return false;
        }
        return keyVersion == other.keyVersion
                && Objects.equals(conversationId, other.conversationId)
                && Objects.equals(userId, other.userId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(conversationId, userId, keyVersion);
    }
}
