package com.thechat.conversation;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.thechat.conversation.dto.ConversationDetailResponse;
import com.thechat.conversation.dto.ConversationResponse;
import com.thechat.conversation.dto.CreateConversationRequest;
import com.thechat.conversation.dto.UpdateGroupConversationRequest;
import com.thechat.realtime.RealtimePublisher;
import com.thechat.user.UserProfile;
import com.thechat.user.UserServiceClient;

/**
 * Phase 3: ConversationService no longer imports UserRepository or
 * FriendshipRepository.
 * User profiles are fetched in a single batch HTTP call (avoids network N+1).
 * Friendship writes delegate to User service via
 * UserServiceClient.ensureFriendship().
 */
@Service
public class ConversationService {

    private static final Logger log = LoggerFactory.getLogger(ConversationService.class);

    private final ConversationRepository conversationRepository;
    private final UserServiceClient userServiceClient;
    private final RealtimePublisher realtimePublisher;

    public ConversationService(
            ConversationRepository conversationRepository,
            UserServiceClient userServiceClient,
            RealtimePublisher realtimePublisher) {
        this.conversationRepository = conversationRepository;
        this.userServiceClient = userServiceClient;
        this.realtimePublisher = realtimePublisher;
    }

    @Transactional(readOnly = true)
    public List<ConversationResponse> getUserConversations(UUID currentUserId) {
        List<Conversation> conversations = conversationRepository
                .findAllByUserIdWithParticipants(currentUserId);

        List<UUID> allUserIds = conversations.stream()
                .flatMap(c -> c.getParticipants().stream().map(ConversationParticipant::getUserId))
                .distinct()
                .toList();

        Map<UUID, UserProfile> profileMap = userServiceClient.batchGetByIds(allUserIds);

        return conversations.stream()
                .map(c -> ConversationResponse.from(c, currentUserId, profileMap))
                .toList();
    }

    @Transactional(readOnly = true)
    public ConversationDetailResponse getUserConversation(UUID conversationId, UUID currentUserId) {
        Conversation conversation = conversationRepository
                .findByIdWithParticipants(conversationId)
                .orElseThrow(() -> new ConversationNotFoundException(conversationId));

        boolean isParticipant = conversation.getParticipants().stream()
                .anyMatch(p -> p.getUserId().equals(currentUserId));
        if (!isParticipant) {
            throw new ConversationNotFoundException(conversationId);
        }

        List<UUID> participantIds = conversation.getParticipants().stream()
                .map(ConversationParticipant::getUserId)
                .toList();
        Map<UUID, UserProfile> profileMap = userServiceClient.batchGetByIds(participantIds);

        return ConversationDetailResponse.from(conversation, currentUserId, profileMap);
    }

    @Transactional
    public ConversationResponse createConversation(UUID currentUserId, CreateConversationRequest request) {
        return switch (request.type()) {
            case DIRECT -> createDirectConversation(currentUserId, request.userId());
            case GROUP -> createGroupConversation(currentUserId, request);
        };
    }

    @Transactional
    public ConversationResponse updateGroupConversation(
            UUID currentUserId,
            UUID conversationId,
            UpdateGroupConversationRequest request) {
        Conversation conversation = conversationRepository
                .findByIdWithParticipants(conversationId)
                .orElseThrow(() -> new ConversationNotFoundException(conversationId));

        boolean isParticipant = conversation.getParticipants().stream()
                .anyMatch(p -> p.getUserId().equals(currentUserId));
        if (!isParticipant) {
            throw new ForbiddenUserException();
        }

        if (conversation.getType() != ConversationType.GROUP) {
            throw new IllegalArgumentException("Only GROUP conversations can be updated");
        }

        if (!conversation.getCreatedBy().equals(currentUserId)) {
            throw new ForbiddenUserException();
        }

        if (request.name() != null && !request.name().isBlank()
                && !request.name().equals(conversation.getName())) {
            conversation.setName(request.name().trim());
        }

        if (request.about() != null && !request.about().isBlank()) {
            conversation.setAbout(request.about().trim());
        }
        ConversationResponse conversationResponse = ConversationResponse.from(conversation, currentUserId, Map.of());
        List<UUID> participantIds = conversation.getParticipants().stream()
                .map(ConversationParticipant::getUserId)
                .toList();

        // Persist is source of truth; WS fan-out is best-effort after commit.
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    publishGroupUpdateBestEffort(conversationId, participantIds, conversationResponse);
                }
            });
        } else {
            publishGroupUpdateBestEffort(conversationId, participantIds, conversationResponse);
        }

        return conversationResponse;
    }

    private void publishGroupUpdateBestEffort(
            UUID conversationId,
            List<UUID> participantIds,
            ConversationResponse conversationResponse) {
        try {
            realtimePublisher.publishGroupUpdate(participantIds, conversationResponse);
        } catch (Exception ex) {
            log.error("Failed to broadcast group update for conversation {}", conversationId, ex);
        }
    }

    private ConversationResponse createDirectConversation(UUID currentUserId, UUID targetUserId) {
        if (currentUserId.equals(targetUserId)) {
            throw new IllegalArgumentException("Cannot create a conversation with yourself");
        }

        Map<UUID, UserProfile> profileMap = userServiceClient.batchGetByIds(
                List.of(currentUserId, targetUserId));
        if (!profileMap.containsKey(currentUserId) || !profileMap.containsKey(targetUserId)) {
            UUID missing = !profileMap.containsKey(currentUserId) ? currentUserId : targetUserId;
            throw new IllegalArgumentException("User not found: " + missing);
        }

        String directKey = generateDirectKey(currentUserId, targetUserId);
        Conversation conversation = conversationRepository.findByDirectKey(directKey).orElse(null);

        if (conversation == null) {
            try {
                Conversation newConversation = new Conversation(
                        ConversationType.DIRECT, null, null, null, currentUserId, directKey);
                newConversation.addParticipant(new ConversationParticipant(newConversation, currentUserId));
                newConversation.addParticipant(new ConversationParticipant(newConversation, targetUserId));
                conversation = conversationRepository.save(newConversation);
            } catch (DataIntegrityViolationException e) {
                conversation = conversationRepository.findByDirectKey(directKey).orElseThrow(() -> e);
            }
        }

        userServiceClient.ensureFriendship(currentUserId, targetUserId);

        conversation = conversationRepository
                .findByIdWithParticipants(conversation.getId())
                .orElse(conversation);

        return ConversationResponse.from(conversation, currentUserId, profileMap);
    }

    private ConversationResponse createGroupConversation(UUID currentUserId, CreateConversationRequest request) {
        Set<UUID> participantIds = new LinkedHashSet<>(request.participants());
        participantIds.remove(currentUserId);

        if (participantIds.isEmpty()) {
            throw new IllegalArgumentException("Group must include at least one other participant");
        }

        Set<UUID> allIds = new HashSet<>(participantIds);
        allIds.add(currentUserId);
        Map<UUID, UserProfile> profileMap = userServiceClient.batchGetByIds(List.copyOf(allIds));

        Set<UUID> missing = allIds.stream()
                .filter(id -> !profileMap.containsKey(id))
                .collect(Collectors.toSet());
        if (!missing.isEmpty()) {
            throw new IllegalArgumentException("Users not found: " + missing);
        }

        String about = request.about() == null || request.about().isBlank()
                ? null
                : request.about().trim();

        Conversation conversation = new Conversation(
                ConversationType.GROUP, request.name().trim(), about, request.image(), currentUserId, null);

        conversation.addParticipant(new ConversationParticipant(conversation, currentUserId));
        for (UUID participantId : participantIds) {
            conversation.addParticipant(new ConversationParticipant(conversation, participantId));
        }

        conversation = conversationRepository.save(conversation);
        conversation = conversationRepository
                .findByIdWithParticipants(conversation.getId())
                .orElse(conversation);

        return ConversationResponse.from(conversation, currentUserId, profileMap);
    }

    private String generateDirectKey(UUID u1, UUID u2) {
        String id1 = u1.toString();
        String id2 = u2.toString();
        return id1.compareTo(id2) < 0
                ? "direct_" + id1 + "_" + id2
                : "direct_" + id2 + "_" + id1;
    }
}
