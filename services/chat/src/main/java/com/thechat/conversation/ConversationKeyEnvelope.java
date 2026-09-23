package com.thechat.conversation;

import java.time.Instant;
import java.util.UUID;

import org.springframework.data.domain.Persistable;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

@Entity
@Table(name = "conversation_key_envelopes")
@IdClass(ConversationKeyEnvelopeId.class)
public class ConversationKeyEnvelope implements Persistable<ConversationKeyEnvelopeId> {

    @Id
    @Column(name = "conversation_id", nullable = false)
    private UUID conversationId;

    @Id
    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Id
    @Column(name = "key_version", nullable = false)
    private int keyVersion;

    @Column(name = "wrapped_key", nullable = false, columnDefinition = "TEXT")
    private String wrappedKey;

    @Column(name = "wrap_nonce", nullable = false, columnDefinition = "TEXT")
    private String wrapNonce;

    @Column(name = "eph_public_key", nullable = false, columnDefinition = "TEXT")
    private String ephPublicKey;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Transient
    private boolean isNew = true;

    protected ConversationKeyEnvelope() {
    }

    public ConversationKeyEnvelope(
            UUID conversationId,
            UUID userId,
            int keyVersion,
            String wrappedKey,
            String wrapNonce,
            String ephPublicKey) {
        this.conversationId = conversationId;
        this.userId = userId;
        this.keyVersion = keyVersion;
        this.wrappedKey = wrappedKey;
        this.wrapNonce = wrapNonce;
        this.ephPublicKey = ephPublicKey;
        this.isNew = true;
    }

    @Override
    public ConversationKeyEnvelopeId getId() {
        return new ConversationKeyEnvelopeId(conversationId, userId, keyVersion);
    }

    @Override
    public boolean isNew() {
        return isNew;
    }

    @PostLoad
    @PostPersist
    void markNotNew() {
        this.isNew = false;
    }

    @PrePersist
    void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = Instant.now();
        }
        this.isNew = false;
    }

    public UUID getConversationId() {
        return conversationId;
    }

    public UUID getUserId() {
        return userId;
    }

    public int getKeyVersion() {
        return keyVersion;
    }

    public String getWrappedKey() {
        return wrappedKey;
    }

    public String getWrapNonce() {
        return wrapNonce;
    }

    public String getEphPublicKey() {
        return ephPublicKey;
    }
}
