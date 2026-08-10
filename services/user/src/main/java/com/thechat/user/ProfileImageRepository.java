package com.thechat.user;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ProfileImageRepository extends JpaRepository<ProfileImage, UUID> {

    Optional<ProfileImage> findByIdAndUser_Id(UUID id, UUID userId);
}
