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

import com.thechat.conversation.dto.ConversationDetailResponse;
import com.thechat.conversation.dto.ConversationResponse;
import com.thechat.conversation.dto.CreateConversationRequest;
import com.thechat.user.UserProfile;
import com.thechat.user.UserServiceClient;

/**
 * Phase 3: ConversationService no longer imports UserRepository or FriendshipRepository.
 * User profiles are fetched in a single batch HTTP call (avoids network N+1).
 * Friendship writes delegate to User service via UserServiceClient.ensureFriendship().
 */
@Service
public class ConversationService {

    private final ConversationRepository conversationRepository;
    private final UserServiceClient userServiceClient;

    public ConversationService(
            ConversationRepository conversationRepository,
            UserServiceClient userServiceClient) {
        this.conversationRepository = conversationRepository;
        this.userServiceClient = userServiceClient;
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

    private ConversationResponse createDirectConversation(UUID currentUserId, UUID targetUserId) {
        if (currentUserId.equals(targetUserId)) {
            throw new IllegalArgumentException("Cannot create a conversation with yourself");
        }

        // Verify both users exist via batch fetch
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

        // Friendship write moves to User service (Phase 3 seam)
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

        // Verify all participants exist via batch fetch
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
                ? null : request.about().trim();

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
