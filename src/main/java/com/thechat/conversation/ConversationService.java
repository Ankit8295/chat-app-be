package com.thechat.conversation;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.thechat.conversation.dto.ConversationDetailResponse;
import com.thechat.conversation.dto.ConversationResponse;
import com.thechat.conversation.dto.CreateConversationRequest;
import com.thechat.friendship.Friendship;
import com.thechat.friendship.FriendshipRepository;
import com.thechat.friendship.FriendshipStatus;
import com.thechat.user.AppUser;
import com.thechat.user.UserNotFoundException;
import com.thechat.user.UserRepository;

@Service
public class ConversationService {

    private final ConversationRepository conversationRepository;
    private final UserRepository userRepository;
    private final FriendshipRepository friendshipRepository;

    public ConversationService(
            ConversationRepository conversationRepository,
            UserRepository userRepository,
            FriendshipRepository friendshipRepository) {
        this.conversationRepository = conversationRepository;
        this.userRepository = userRepository;
        this.friendshipRepository = friendshipRepository;
    }

    @Transactional(readOnly = true)
    public List<ConversationResponse> getUserConversations(UUID currentUserId) {
        List<Conversation> conversations = conversationRepository
                .findAllByUserIdWithParticipantsAndUsers(currentUserId);
        return conversations.stream()
                .map(c -> ConversationResponse.from(c, currentUserId))
                .toList();
    }

    @Transactional(readOnly = true)
    public ConversationDetailResponse getUserConversation(UUID conversationId, UUID currentUserId) {
        Conversation conversation = conversationRepository
                .findByIdWithParticipantsAndUsers(conversationId)
                .orElseThrow(() -> new ConversationNotFoundException(conversationId));

        boolean isParticipant = conversation.getParticipants().stream()
                .anyMatch(p -> p.getUser().getId().equals(currentUserId));
        if (!isParticipant) {
            throw new ConversationNotFoundException(conversationId);
        }

        return ConversationDetailResponse.from(conversation, currentUserId);
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

        AppUser currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new UserNotFoundException(currentUserId));
        AppUser targetUser = userRepository.findById(targetUserId)
                .orElseThrow(() -> new UserNotFoundException(targetUserId));

        String directKey = generateDirectKey(currentUserId, targetUserId);

        Conversation conversation = conversationRepository.findByDirectKey(directKey).orElse(null);

        if (conversation == null) {
            try {
                Conversation newConversation = new Conversation(
                        ConversationType.DIRECT,
                        null,
                        null,
                        null,
                        currentUserId,
                        directKey);

                ConversationParticipant p1 = new ConversationParticipant(newConversation, currentUser);
                ConversationParticipant p2 = new ConversationParticipant(newConversation, targetUser);

                newConversation.addParticipant(p1);
                newConversation.addParticipant(p2);

                conversation = conversationRepository.save(newConversation);
            } catch (DataIntegrityViolationException e) {
                conversation = conversationRepository.findByDirectKey(directKey)
                        .orElseThrow(() -> e);
            }
        }

        ensureBidirectionalFriendship(currentUser, targetUser);

        conversation = conversationRepository
                .findByIdWithParticipantsAndUsers(conversation.getId())
                .orElse(conversation);

        return ConversationResponse.from(conversation, currentUserId);
    }

    private ConversationResponse createGroupConversation(UUID currentUserId, CreateConversationRequest request) {
        AppUser currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new UserNotFoundException(currentUserId));

        Set<UUID> participantIds = new LinkedHashSet<>(request.participants());
        participantIds.remove(currentUserId);

        if (participantIds.isEmpty()) {
            throw new IllegalArgumentException("Group must include at least one other participant");
        }

        List<AppUser> members = userRepository.findAllById(participantIds);
        if (members.size() != participantIds.size()) {
            Set<UUID> foundIds = new HashSet<>();
            for (AppUser member : members) {
                foundIds.add(member.getId());
            }
            UUID missingId = participantIds.stream()
                    .filter(id -> !foundIds.contains(id))
                    .findFirst()
                    .orElseThrow();
            throw new UserNotFoundException(missingId);
        }

        String about = request.about() == null || request.about().isBlank()
                ? null
                : request.about().trim();

        Conversation conversation = new Conversation(
                ConversationType.GROUP,
                request.name().trim(),
                about,
                request.image(),
                currentUserId,
                null);

        conversation.addParticipant(new ConversationParticipant(conversation, currentUser));
        for (AppUser member : members) {
            conversation.addParticipant(new ConversationParticipant(conversation, member));
        }

        conversation = conversationRepository.save(conversation);
        conversation = conversationRepository
                .findByIdWithParticipantsAndUsers(conversation.getId())
                .orElse(conversation);

        return ConversationResponse.from(conversation, currentUserId);
    }

    private void ensureBidirectionalFriendship(AppUser u1, AppUser u2) {
        if (!friendshipRepository.existsByUserIdAndFriendUserId(u1.getId(), u2.getId())) {
            friendshipRepository.save(new Friendship(u1, u2, FriendshipStatus.ACTIVE));
        }
        if (!friendshipRepository.existsByUserIdAndFriendUserId(u2.getId(), u1.getId())) {
            friendshipRepository.save(new Friendship(u2, u1, FriendshipStatus.ACTIVE));
        }
    }

    private String generateDirectKey(UUID u1, UUID u2) {
        String id1 = u1.toString();
        String id2 = u2.toString();
        if (id1.compareTo(id2) < 0) {
            return "direct_" + id1 + "_" + id2;
        } else {
            return "direct_" + id2 + "_" + id1;
        }
    }
}
