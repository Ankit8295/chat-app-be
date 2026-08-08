package com.thechat.friendship;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FriendshipRepository extends JpaRepository<Friendship, UUID> {

    List<Friendship> findByUserIdAndStatus(UUID userId, FriendshipStatus status);

    Optional<Friendship> findByUserIdAndFriendUserId(UUID userId, UUID friendUserId);

    boolean existsByUserIdAndFriendUserId(UUID userId, UUID friendUserId);

    @Query(value = "SELECT f FROM Friendship f JOIN FETCH f.friendUser WHERE f.user.id = :userId AND f.status = :status",
           countQuery = "SELECT COUNT(f) FROM Friendship f WHERE f.user.id = :userId AND f.status = :status")
    Page<Friendship> findAllFriendsWithUserByUserIdAndStatus(
            @Param("userId") UUID userId,
            @Param("status") FriendshipStatus status,
            Pageable pageable
    );
}
