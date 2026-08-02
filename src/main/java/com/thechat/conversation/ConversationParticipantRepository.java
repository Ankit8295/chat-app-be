package com.thechat.conversation;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ConversationParticipantRepository extends JpaRepository<ConversationParticipant, UUID> {

    List<ConversationParticipant> findByUserId(UUID userId);

    boolean existsByConversationIdAndUserId(UUID conversationId, UUID userId);

    @Query("SELECT cp.user.id FROM ConversationParticipant cp WHERE cp.conversation.id = :conversationId")
    List<UUID> findUserIdsByConversationId(@Param("conversationId") UUID conversationId);
}
