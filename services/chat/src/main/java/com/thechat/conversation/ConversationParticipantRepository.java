package com.thechat.conversation;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ConversationParticipantRepository extends JpaRepository<ConversationParticipant, UUID> {

        List<ConversationParticipant> findByUserId(UUID userId);

        boolean existsByConversationIdAndUserId(UUID conversationId, UUID userId);

        Optional<ConversationParticipant> findByConversationIdAndUserId(UUID conversationId, UUID userId);

        @Query("SELECT cp.userId FROM ConversationParticipant cp WHERE cp.conversation.id = :conversationId")
        List<UUID> findUserIdsByConversationId(@Param("conversationId") UUID conversationId);

        @Modifying(clearAutomatically = true)
        @Query("UPDATE ConversationParticipant cp SET cp.hiddenAt = NULL WHERE cp.conversation.id = :conversationId")
        void clearHiddenAtByConversationId(@Param("conversationId") UUID conversationId);

        @Modifying(clearAutomatically = true)
        @Query("UPDATE ConversationParticipant cp SET cp.hiddenAt = NULL WHERE cp.conversation.id = :conversationId AND cp.userId = :userId")
        void clearHiddenAtForUser(@Param("conversationId") UUID conversationId, @Param("userId") UUID userId);
}
