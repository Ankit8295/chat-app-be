package com.thechat.friendship;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FriendshipRepository extends JpaRepository<Friendship, UUID> {

    List<Friendship> findByUserIdAndStatus(UUID userId, FriendshipStatus status);

    Optional<Friendship> findByUserIdAndFriendUserId(UUID userId, UUID friendUserId);

    boolean existsByUserIdAndFriendUserId(UUID userId, UUID friendUserId);

    @Query("SELECT f FROM Friendship f JOIN FETCH f.friendUser WHERE f.user.id = :userId AND f.status = :status")
    List<Friendship> findAllFriendsWithUserByUserIdAndStatus(@Param("userId") UUID userId, @Param("status") FriendshipStatus status);
}
