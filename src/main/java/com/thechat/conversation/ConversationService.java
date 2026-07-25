package com.thechat.conversation;

import java.util.List;
import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.thechat.conversation.dto.ConversationResponse;
import com.thechat.friendship.Friendship;
import com.thechat.friendship.FriendshipRepository;
import com.thechat.friendship.FriendshipStatus;
import com.thechat.user.AppUser;
import com.thechat.user.UserNotFoundException;
import com.thechat.user.UserRepository;

@Service
public class ConversationService {

    private final ConversationRepository conversationRepository;
    private final ConversationParticipantRepository participantRepository;
    private final UserRepository userRepository;
    private final FriendshipRepository friendshipRepository;

    public ConversationService(
            ConversationRepository conversationRepository,
            ConversationParticipantRepository participantRepository,
            UserRepository userRepository,
            FriendshipRepository friendshipRepository) {
        this.conversationRepository = conversationRepository;
        this.participantRepository = participantRepository;
        this.userRepository = userRepository;
        this.friendshipRepository = friendshipRepository;
    }

    @Transactional(readOnly = true)
    public List<ConversationResponse> getUserConversations(UUID currentUserId) {
        List<Conversation> conversations = conversationRepository.findAllByUserIdWithParticipantsAndUsers(currentUserId);
        return conversations.stream()
                .map(c -> ConversationResponse.from(c, currentUserId))
                .toList();
    }

    @Transactional
    public ConversationResponse getOrCreateDirectConversation(UUID currentUserId, UUID targetUserId) {
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
                        currentUserId,
                        directKey
                );

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
