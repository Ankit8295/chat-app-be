package com.thechat.user;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ProfileImageRepository extends JpaRepository<ProfileImage, UUID> {

    Optional<ProfileImage> findByIdAndUser_Id(UUID id, UUID userId);

    List<ProfileImage> findByUser_IdAndStatus(UUID userId, ProfileImageStatus status);

    List<ProfileImage> findByStatusAndCreatedAtBefore(ProfileImageStatus status, Instant cutoff);
}
