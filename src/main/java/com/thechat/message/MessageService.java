package com.thechat.message;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.thechat.AppProperties;
import com.thechat.conversation.Conversation;
import com.thechat.conversation.ConversationNotFoundException;
import com.thechat.conversation.ConversationParticipantRepository;
import com.thechat.conversation.ConversationRepository;
import com.thechat.message.dto.MessageCursor;
import com.thechat.message.dto.MessagePageResponse;
import com.thechat.message.dto.MessageResponse;
import com.thechat.realtime.RealtimePublisher;
import com.thechat.user.AppUser;
import com.thechat.user.UserNotFoundException;
import com.thechat.user.UserRepository;

@Service
public class MessageService {

    private static final Logger log = LoggerFactory.getLogger(MessageService.class);

    private static final int DEFAULT_LIMIT = 50;
    private static final int MAX_LIMIT = 100;
    private static final int MAX_CONTENT_LENGTH = 4000;

    private final MessageRepository messageRepository;
    private final ConversationParticipantRepository conversationParticipantRepository;
    private final ConversationRepository conversationRepository;
    private final UserRepository userRepository;
    private final RealtimePublisher realtimePublisher;
    private final MessagePersistenceService messagePersistenceService;
    private final String instanceId;

    public MessageService(
            MessageRepository messageRepository,
            ConversationParticipantRepository conversationParticipantRepository,
            ConversationRepository conversationRepository,
            UserRepository userRepository,
            RealtimePublisher realtimePublisher,
            MessagePersistenceService messagePersistenceService,
            AppProperties appProperties) {
        this.messageRepository = messageRepository;
        this.conversationParticipantRepository = conversationParticipantRepository;
        this.conversationRepository = conversationRepository;
        this.userRepository = userRepository;
        this.realtimePublisher = realtimePublisher;
        this.messagePersistenceService = messagePersistenceService;
        this.instanceId = appProperties.id();
    }

    @Transactional(readOnly = true)
    public MessagePageResponse getMessages(
            UUID conversationId,
            UUID currentUserId,
            String prevCursorRaw,
            String nextCursorRaw,
            Integer limit) {
        assertParticipant(conversationId, currentUserId);

        boolean hasPrevParam = blankToNull(prevCursorRaw) != null;
        boolean hasNextParam = blankToNull(nextCursorRaw) != null;
        if (hasPrevParam && hasNextParam) {
            throw new IllegalArgumentException("Provide only one of prevCursor or nextCursor");
        }

        int pageSize = normalizeLimit(limit);
        MessageCursor prevParam = MessageCursor.decode(blankToNull(prevCursorRaw));
        MessageCursor nextParam = MessageCursor.decode(blankToNull(nextCursorRaw));

        List<Message> page;
        boolean hasOlder;
        boolean hasNewer;

        if (prevParam != null) {
            List<Message> rows = messageRepository.findPageAfter(
                    conversationId,
                    prevParam.createdAt(),
                    prevParam.id(),
                    PageRequest.of(0, pageSize + 1));
            hasNewer = rows.size() > pageSize;
            List<Message> ascending = hasNewer ? rows.subList(0, pageSize) : rows;
            page = new ArrayList<>(ascending);
            Collections.reverse(page);
            hasOlder = true;
        } else if (nextParam != null) {
            List<Message> rows = messageRepository.findPageBefore(
                    conversationId,
                    nextParam.createdAt(),
                    nextParam.id(),
                    PageRequest.of(0, pageSize + 1));
            hasOlder = rows.size() > pageSize;
            page = hasOlder ? new ArrayList<>(rows.subList(0, pageSize)) : new ArrayList<>(rows);
            hasNewer = true;
        } else {
            List<Message> rows = messageRepository.findLatestPage(
                    conversationId,
                    PageRequest.of(0, pageSize + 1));
            hasOlder = rows.size() > pageSize;
            page = hasOlder ? new ArrayList<>(rows.subList(0, pageSize)) : new ArrayList<>(rows);
            hasNewer = false;
        }

        List<MessageResponse> items = page.stream()
                .map(MessageResponse::from)
                .toList();

        String responsePrevCursor = null;
        String responseNextCursor = null;
        if (!page.isEmpty()) {
            Message newest = page.get(0);
            Message oldest = page.get(page.size() - 1);
            if (hasNewer) {
                responsePrevCursor = MessageCursor.from(newest.getCreatedAt(), newest.getId()).encode();
            }
            if (hasOlder) {
                responseNextCursor = MessageCursor.from(oldest.getCreatedAt(), oldest.getId()).encode();
            }
        }

        return new MessagePageResponse(items, responsePrevCursor, responseNextCursor);
    }

    public MessageResponse acceptAndBroadcast(UUID senderId, UUID conversationId, String rawContent) {
        String content = normalizeContent(rawContent);
        assertParticipant(conversationId, senderId);

        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ConversationNotFoundException(conversationId));
        AppUser sender = userRepository.findById(senderId)
                .orElseThrow(() -> new UserNotFoundException(senderId));

        Instant createdAt = Instant.now();
        Message message = new Message(UUID.randomUUID(), conversation, sender, content, createdAt);
        MessageResponse response = MessageResponse.from(message);

        List<UUID> participantIds = conversationParticipantRepository.findUserIdsByConversationId(conversationId);
        log.info("[instance-{}] Broadcasting message {} from user {} to {} participants in conversation {}",
                instanceId, response.id(), senderId, participantIds.size(), conversationId);
        try {
            realtimePublisher.publishMessageNew(participantIds, response);
        } catch (Exception ex) {
            log.error("Failed to broadcast message {} to conversation {}", response.id(), conversationId, ex);
            throw new IllegalStateException("Failed to broadcast message", ex);
        }

        try {
            messagePersistenceService.saveMessageAndTouchConversation(message, conversation);
        } catch (Exception ex) {
            log.error(
                    "Failed to persist message {} in conversation {} after broadcast",
                    response.id(),
                    conversationId,
                    ex);
        }

        return response;
    }

    private String normalizeContent(String rawContent) {
        if (rawContent == null || rawContent.isBlank()) {
            throw new IllegalArgumentException("Message content is required");
        }
        String content = rawContent.trim();
        if (content.length() > MAX_CONTENT_LENGTH) {
            throw new IllegalArgumentException("Message content must be at most " + MAX_CONTENT_LENGTH + " characters");
        }
        return content;
    }

    private void assertParticipant(UUID conversationId, UUID currentUserId) {
        if (!conversationParticipantRepository.existsByConversationIdAndUserId(conversationId, currentUserId)) {
            throw new ConversationNotFoundException(conversationId);
        }
    }

    private int normalizeLimit(Integer limit) {
        if (limit == null || limit <= 0) {
            return DEFAULT_LIMIT;
        }
        return Math.min(limit, MAX_LIMIT);
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
