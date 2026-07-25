package com.thechat.user;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.thechat.friendship.Friendship;
import com.thechat.friendship.FriendshipRepository;
import com.thechat.friendship.FriendshipStatus;
import com.thechat.friendship.dto.FriendResponse;

@Service
public class UserService {
    private final UserRepository userRepository;
    private final FriendshipRepository friendshipRepository;

    public UserService(UserRepository userRepository, FriendshipRepository friendshipRepository) {
        this.userRepository = userRepository;
        this.friendshipRepository = friendshipRepository;
    }

    @Transactional(readOnly = true)
    public List<UserSearchResultResponse> searchUsers(UUID requesterId, String search) {
        List<AppUser> matchedUsers = userRepository.searchUsersExcludingSelf(requesterId, search);
        List<Friendship> friendships = friendshipRepository.findByUserIdAndStatus(requesterId, FriendshipStatus.ACTIVE);

        Map<UUID, String> friendshipStatusMap = friendships.stream()
                .collect(Collectors.toMap(
                        f -> f.getFriendUser().getId(),
                        f -> f.getStatus().name().toLowerCase()
                ));

        return matchedUsers.stream()
                .map(user -> {
                    String status = friendshipStatusMap.getOrDefault(user.getId(), "none");
                    return UserSearchResultResponse.of(user, status);
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public List<FriendResponse> getFriends(UUID requesterId) {
        List<Friendship> friends = friendshipRepository.findAllFriendsWithUserByUserIdAndStatus(requesterId, FriendshipStatus.ACTIVE);
        return friends.stream()
                .map(FriendResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public UserResponse getUserById(UUID id) {
        return userRepository.findById(id)
                .map(UserResponse::from)
                .orElseThrow(() -> new UserNotFoundException(id));
    }
}
