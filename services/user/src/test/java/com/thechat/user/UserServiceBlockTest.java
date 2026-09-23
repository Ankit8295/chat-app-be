package com.thechat.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.thechat.friendship.Friendship;
import com.thechat.friendship.FriendshipRepository;
import com.thechat.friendship.FriendshipStatus;
import com.thechat.friendship.dto.FriendshipStatusResponse;
import com.thechat.object_storage.CloudflareR2Client;

@ExtendWith(MockitoExtension.class)
class UserServiceBlockTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private FriendshipRepository friendshipRepository;

    @Mock
    private UserPreferenceRepository userPreferenceRepository;

    @Mock
    private ProfileImageRepository profileImageRepository;

    @Mock
    private ProfileImageCleanupService profileImageCleanupService;

    @Mock
    private CloudflareR2Client r2Client;

    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserService(
                userRepository,
                friendshipRepository,
                userPreferenceRepository,
                profileImageRepository,
                profileImageCleanupService,
                r2Client);
    }

    @Test
    void blockUserWritesBlockedAndClearsActiveIncoming() {
        UUID requesterId = UUID.randomUUID();
        UUID targetId = UUID.randomUUID();
        AppUser requester = new AppUser(requesterId, "a@test.com", "A");
        AppUser target = new AppUser(targetId, "b@test.com", "B");
        Friendship outgoingActive = new Friendship(requester, target, FriendshipStatus.ACTIVE);
        Friendship incomingActive = new Friendship(target, requester, FriendshipStatus.ACTIVE);

        when(userRepository.findById(requesterId)).thenReturn(Optional.of(requester));
        when(userRepository.findById(targetId)).thenReturn(Optional.of(target));
        when(friendshipRepository.findByUserIdAndFriendUserId(requesterId, targetId))
                .thenReturn(Optional.of(outgoingActive));
        when(friendshipRepository.findByUserIdAndFriendUserId(targetId, requesterId))
                .thenReturn(Optional.of(incomingActive));

        userService.blockUser(requesterId, targetId);

        assertThat(outgoingActive.getStatus()).isEqualTo(FriendshipStatus.BLOCKED);
        verify(friendshipRepository).deleteByUserIdAndFriendUserId(targetId, requesterId);
        verify(friendshipRepository, never()).save(any());
    }

    @Test
    void blockUserRejectsSelf() {
        UUID id = UUID.randomUUID();
        assertThatThrownBy(() -> userService.blockUser(id, id))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Cannot block yourself");
    }

    @Test
    void unblockUserDeletesOwnBlockedEdge() {
        UUID requesterId = UUID.randomUUID();
        UUID targetId = UUID.randomUUID();
        AppUser requester = new AppUser(requesterId, "a@test.com", "A");
        AppUser target = new AppUser(targetId, "b@test.com", "B");
        Friendship blocked = new Friendship(requester, target, FriendshipStatus.BLOCKED);

        when(friendshipRepository.findByUserIdAndFriendUserId(requesterId, targetId))
                .thenReturn(Optional.of(blocked));

        userService.unblockUser(requesterId, targetId);

        verify(friendshipRepository).deleteByUserIdAndFriendUserId(requesterId, targetId);
    }

    @Test
    void unblockUserForbiddenWhenOnlyPeerBlocked() {
        UUID requesterId = UUID.randomUUID();
        UUID targetId = UUID.randomUUID();

        when(friendshipRepository.findByUserIdAndFriendUserId(requesterId, targetId))
                .thenReturn(Optional.empty());
        when(friendshipRepository.existsByUserIdAndFriendUserIdAndStatus(
                targetId, requesterId, FriendshipStatus.BLOCKED))
                .thenReturn(true);

        assertThatThrownBy(() -> userService.unblockUser(requesterId, targetId))
                .isInstanceOf(ForbiddenOperationException.class);
    }

    @Test
    void ensureFriendshipNoOpWhenBlocked() {
        UUID userId = UUID.randomUUID();
        UUID friendId = UUID.randomUUID();

        when(friendshipRepository.existsByUserIdAndFriendUserIdAndStatus(
                userId, friendId, FriendshipStatus.BLOCKED))
                .thenReturn(true);

        userService.ensureFriendship(userId, friendId);

        verify(userRepository, never()).findById(any());
        verify(friendshipRepository, never()).save(any());
    }

    @Test
    void getFriendshipStatusReturnsBlockedFlagsForMutualBlock() {
        UUID userId = UUID.randomUUID();
        UUID otherId = UUID.randomUUID();
        AppUser user = new AppUser(userId, "a@test.com", "A");
        AppUser other = new AppUser(otherId, "b@test.com", "B");

        when(friendshipRepository.findByUserIdAndFriendUserId(userId, otherId))
                .thenReturn(Optional.of(new Friendship(user, other, FriendshipStatus.BLOCKED)));
        when(friendshipRepository.findByUserIdAndFriendUserId(otherId, userId))
                .thenReturn(Optional.of(new Friendship(other, user, FriendshipStatus.BLOCKED)));

        FriendshipStatusResponse status = userService.getFriendshipStatus(userId, otherId);

        assertThat(status.status()).isEqualTo("blocked");
        assertThat(status.blockedByMe()).isTrue();
        assertThat(status.blockedByPeer()).isTrue();
    }
}
