package com.thechat.conversation;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ConversationRepository extends JpaRepository<Conversation, UUID> {

        Optional<Conversation> findByDirectKey(String directKey);

        // Phase 3: no JOIN FETCH p.user — user data comes from UserServiceClient
        @Query("SELECT DISTINCT c FROM Conversation c " +
                        "JOIN FETCH c.participants p " +
                        "WHERE c.id = :conversationId")
        Optional<Conversation> findByIdWithParticipants(@Param("conversationId") UUID conversationId);

        @Query("SELECT DISTINCT c FROM Conversation c " +
                        "JOIN FETCH c.participants p " +
                        "WHERE c.id IN (SELECT cp.conversation.id FROM ConversationParticipant cp WHERE cp.userId = :userId) "
                        +
                        "ORDER BY c.updatedAt DESC")
        List<Conversation> findAllByUserIdWithParticipants(@Param("userId") UUID userId);
}
