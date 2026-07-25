package com.thechat.conversation;

import java.time.Instant;
import java.util.UUID;

import com.thechat.user.AppUser;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
    name = "conversation_participants",
    uniqueConstraints = {
        @UniqueConstraint(name = "uq_conversation_participants", columnNames = {"conversation_id", "user_id"})
    }
)
public class ConversationParticipant {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "conversation_id", nullable = false)
    private Conversation conversation;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @Column(nullable = false, updatable = false)
    private Instant joinedAt;

    protected ConversationParticipant() {
    }

    public ConversationParticipant(Conversation conversation, AppUser user) {
        this.id = UUID.randomUUID();
        this.conversation = conversation;
        this.user = user;
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

    public AppUser getUser() {
        return user;
    }

    public Instant getJoinedAt() {
        return joinedAt;
    }
}
