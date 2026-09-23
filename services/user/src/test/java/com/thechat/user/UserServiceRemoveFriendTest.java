package com.thechat.user;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.thechat.friendship.FriendshipRepository;
import com.thechat.object_storage.CloudflareR2Client;

@ExtendWith(MockitoExtension.class)
class UserServiceRemoveFriendTest {

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
    void removeFriendDeletesBothDirections() {
        UUID requesterId = UUID.randomUUID();
        UUID friendUserId = UUID.randomUUID();

        userService.removeFriend(requesterId, friendUserId);

        verify(friendshipRepository).deleteByUserIdAndFriendUserId(requesterId, friendUserId);
        verify(friendshipRepository).deleteByUserIdAndFriendUserId(friendUserId, requesterId);
        verify(userRepository, never()).findById(any());
    }

    @Test
    void removeFriendIsIdempotentWhenNoRowsExist() {
        UUID requesterId = UUID.randomUUID();
        UUID friendUserId = UUID.randomUUID();

        userService.removeFriend(requesterId, friendUserId);
        userService.removeFriend(requesterId, friendUserId);

        verify(friendshipRepository, times(2))
                .deleteByUserIdAndFriendUserId(requesterId, friendUserId);
        verify(friendshipRepository, times(2))
                .deleteByUserIdAndFriendUserId(friendUserId, requesterId);
    }

    @Test
    void removeFriendRejectsSelf() {
        UUID requesterId = UUID.randomUUID();

        assertThatThrownBy(() -> userService.removeFriend(requesterId, requesterId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Cannot remove yourself as a friend");

        verify(friendshipRepository, never()).deleteByUserIdAndFriendUserId(any(), any());
    }
}
