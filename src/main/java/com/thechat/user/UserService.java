package com.thechat.user;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.thechat.common.dto.PageResponse;
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
    public PageResponse<UserSearchResultResponse> searchUsers(UUID requesterId, String search, int page, int size) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "name"));
        Page<AppUser> matchedUsersPage = userRepository.searchUsersExcludingSelf(requesterId, search, pageable);

        List<Friendship> friendships = friendshipRepository.findByUserIdAndStatus(requesterId, FriendshipStatus.ACTIVE);

        Map<UUID, String> friendshipStatusMap = friendships.stream()
                .collect(Collectors.toMap(
                        f -> f.getFriendUser().getId(),
                        f -> f.getStatus().name().toLowerCase()
                ));

        List<UserSearchResultResponse> content = matchedUsersPage.getContent().stream()
                .map(user -> {
                    String status = friendshipStatusMap.getOrDefault(user.getId(), "none");
                    return UserSearchResultResponse.of(user, status);
                })
                .toList();

        return PageResponse.of(content, page, size, matchedUsersPage.getTotalElements());
    }

    @Transactional(readOnly = true)
    public PageResponse<FriendResponse> getFriends(UUID requesterId, int page, int size) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Friendship> friendsPage = friendshipRepository.findAllFriendsWithUserByUserIdAndStatus(requesterId, FriendshipStatus.ACTIVE, pageable);

        List<FriendResponse> content = friendsPage.getContent().stream()
                .map(FriendResponse::from)
                .toList();

        return PageResponse.of(content, page, size, friendsPage.getTotalElements());
    }

    @Transactional(readOnly = true)
    public UserResponse getUserById(UUID id) {
        return userRepository.findById(id)
                .map(UserResponse::from)
                .orElseThrow(() -> new UserNotFoundException(id));
    }
}
