package com.thechat.message;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MessageRepository extends JpaRepository<Message, UUID> {

    @Query("""
            SELECT m FROM Message m
            JOIN FETCH m.sender
            JOIN FETCH m.conversation
            WHERE m.conversation.id = :conversationId
            ORDER BY m.createdAt DESC, m.id DESC
            """)
    List<Message> findLatestPage(
            @Param("conversationId") UUID conversationId,
            Pageable pageable);

    @Query("""
            SELECT m FROM Message m
            JOIN FETCH m.sender
            JOIN FETCH m.conversation
            WHERE m.conversation.id = :conversationId
              AND (
                    m.createdAt < :beforeCreatedAt
                    OR (m.createdAt = :beforeCreatedAt AND m.id < :beforeId)
                  )
            ORDER BY m.createdAt DESC, m.id DESC
            """)
    List<Message> findPageBefore(
            @Param("conversationId") UUID conversationId,
            @Param("beforeCreatedAt") Instant beforeCreatedAt,
            @Param("beforeId") UUID beforeId,
            Pageable pageable);

    @Query("""
            SELECT m FROM Message m
            JOIN FETCH m.sender
            JOIN FETCH m.conversation
            WHERE m.conversation.id = :conversationId
              AND (
                    m.createdAt > :afterCreatedAt
                    OR (m.createdAt = :afterCreatedAt AND m.id > :afterId)
                  )
            ORDER BY m.createdAt ASC, m.id ASC
            """)
    List<Message> findPageAfter(
            @Param("conversationId") UUID conversationId,
            @Param("afterCreatedAt") Instant afterCreatedAt,
            @Param("afterId") UUID afterId,
            Pageable pageable);
}
