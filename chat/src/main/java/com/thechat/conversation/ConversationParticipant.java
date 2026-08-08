package com.thechat.conversation;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * Phase 3: stores userId as a plain UUID — no JPA foreign key to app_users.
 * User profiles are fetched via UserServiceClient when building responses.
 * Cross-DB foreign keys are forbidden in database-per-service architecture.
 */
@Entity
@Table(name = "conversation_participants", uniqueConstraints = {
        @UniqueConstraint(name = "uq_conversation_participants", columnNames = { "conversation_id", "user_id" })
})
public class ConversationParticipant {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "conversation_id", nullable = false)
    private Conversation conversation;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(nullable = false, updatable = false)
    private Instant joinedAt;

    protected ConversationParticipant() {
    }

    public ConversationParticipant(Conversation conversation, UUID userId) {
        this.id = UUID.randomUUID();
        this.conversation = conversation;
        this.userId = userId;
    }

    @PrePersist
    void onCreate() {
        this.joinedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public Conversation getConversation() {
        return conversation;
    }

    public UUID getUserId() {
        return userId;
    }

    public Instant getJoinedAt() {
        return joinedAt;
    }
}
