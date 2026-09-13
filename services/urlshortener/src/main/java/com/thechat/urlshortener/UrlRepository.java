package com.thechat.urlshortener;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface UrlRepository extends JpaRepository<UrlEntity, UUID> {

    Optional<UrlEntity> findByShortCode(String shortCode);

    boolean existsByShortCode(String shortCode);

    Optional<UrlEntity> findByUserIdAndLongUrl(UUID userId, String longUrl);

    Optional<UrlEntity> findByUserIdIsNullAndLongUrl(String longUrl);

    List<UrlEntity> findByUserIdOrderByCreatedAtDesc(UUID userId);
}
