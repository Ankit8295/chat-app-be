package com.thechat.conversation;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ConversationRepository extends JpaRepository<Conversation, UUID> {

    Optional<Conversation> findByDirectKey(String directKey);

    @Query("SELECT DISTINCT c FROM Conversation c " +
           "JOIN FETCH c.participants p " +
           "JOIN FETCH p.user u " +
           "WHERE c.id IN (SELECT cp.conversation.id FROM ConversationParticipant cp WHERE cp.user.id = :userId) " +
           "ORDER BY c.updatedAt DESC")
    List<Conversation> findAllByUserIdWithParticipantsAndUsers(@Param("userId") UUID userId);
}
