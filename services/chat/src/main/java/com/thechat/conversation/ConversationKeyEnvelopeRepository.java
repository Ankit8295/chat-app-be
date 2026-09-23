package com.thechat.conversation;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ConversationKeyEnvelopeRepository
        extends JpaRepository<ConversationKeyEnvelope, ConversationKeyEnvelopeId> {

    boolean existsByConversationIdAndKeyVersion(UUID conversationId, int keyVersion);

    List<ConversationKeyEnvelope> findByConversationIdAndUserId(UUID conversationId, UUID userId);
}
