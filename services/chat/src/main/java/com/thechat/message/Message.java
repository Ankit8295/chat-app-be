package com.thechat.message;

import java.time.Instant;
import java.util.UUID;

import com.thechat.conversation.Conversation;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

/**
 * Phase 3: stores senderId as a plain UUID — no JPA foreign key to app_users.
 * Sender name/image are fetched via UserServiceClient when building responses.
 * New rows store ciphertext only; content remains for legacy plaintext.
 */
@Entity
@Table(name = "messages")
public class Message {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "conversation_id", nullable = false)
    private Conversation conversation;

    @Column(name = "sender_id", nullable = false)
    private UUID senderId;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Column(columnDefinition = "TEXT")
    private String ciphertext;

    @Column(columnDefinition = "TEXT")
    private String nonce;

    @Column(name = "key_version")
    private Integer keyVersion;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected Message() {
    }

    public Message(
            UUID id,
            Conversation conversation,
            UUID senderId,
            String ciphertext,
            String nonce,
            Integer keyVersion,
            Instant createdAt) {
        this.id = id;
        this.conversation = conversation;
        this.senderId = senderId;
        this.ciphertext = ciphertext;
        this.nonce = nonce;
        this.keyVersion = keyVersion;
        this.createdAt = createdAt;
    }

    @PrePersist
    void onCreate() {
        if (this.id == null) {
            this.id = UUID.randomUUID();
        }
        if (this.createdAt == null) {
            this.createdAt = Instant.now();
        }
    }

    public UUID getId() {
        return id;
    }

    public Conversation getConversation() {
        return conversation;
    }

    public UUID getSenderId() {
        return senderId;
    }

    public String getContent() {
        return content;
    }

    public String getCiphertext() {
        return ciphertext;
    }

    public String getNonce() {
        return nonce;
    }

    public Integer getKeyVersion() {
        return keyVersion;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
