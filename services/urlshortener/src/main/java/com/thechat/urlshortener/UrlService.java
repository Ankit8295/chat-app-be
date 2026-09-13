package com.thechat.urlshortener;

import java.net.URI;
import java.security.SecureRandom;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.thechat.urlshortener.dto.UrlResponse;

@Service
public class UrlService {

    private static final int SHORT_CODE_LENGTH = 8;
    private static final String CHARACTERS =
            "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";

    private final SecureRandom secureRandom = new SecureRandom();
    private final UrlRepository urlRepository;
    private final UrlCacheStore urlCacheStore;
    private final String baseUrl;

    public UrlService(
            UrlRepository urlRepository,
            UrlCacheStore urlCacheStore,
            @Value("${app.base-url}") String baseUrl) {
        this.urlRepository = urlRepository;
        this.urlCacheStore = urlCacheStore;
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl : baseUrl + "/";
    }

    @Transactional(readOnly = true)
    public String getLongUrl(String shortCode) {
        Optional<String> cached = urlCacheStore.get(shortCode);
        if (cached.isPresent()) {
            return cached.get();
        }

        UrlEntity url = urlRepository.findByShortCode(shortCode)
                .orElseThrow(() -> new UrlNotFoundException(shortCode));

        urlCacheStore.put(shortCode, url.getLongUrl());
        return url.getLongUrl();
    }

    @Transactional
    public UrlResponse shorten(String longUrl, UUID userId) {
        String normalized = longUrl.trim();
        validateUrl(normalized);

        Optional<UrlEntity> existing = userId != null
                ? urlRepository.findByUserIdAndLongUrl(userId, normalized)
                : urlRepository.findByUserIdIsNullAndLongUrl(normalized);

        if (existing.isPresent()) {
            return toResponse(existing.get());
        }

        String shortCode = generateUniqueShortCode();
        UrlEntity url = new UrlEntity(userId, normalized, shortCode);
        urlRepository.save(url);
        urlCacheStore.put(shortCode, normalized);
        return toResponse(url);
    }

    @Transactional(readOnly = true)
    public List<UrlResponse> listMine(UUID userId) {
        return urlRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public void deleteMine(UUID id, UUID userId) {
        UrlEntity url = urlRepository.findById(id)
                .orElseThrow(() -> new UrlNotFoundException(id));

        if (url.getUserId() == null || !url.getUserId().equals(userId)) {
            throw new UrlAccessDeniedException(id);
        }

        urlRepository.delete(url);
        urlCacheStore.delete(url.getShortCode());
    }

    private void validateUrl(String longUrl) {
        try {
            new URI(longUrl).toURL();
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid URL");
        }
    }

    private String generateUniqueShortCode() {
        String shortCode;
        do {
            shortCode = randomCode();
        } while (urlRepository.existsByShortCode(shortCode));
        return shortCode;
    }

    private String randomCode() {
        StringBuilder builder = new StringBuilder(SHORT_CODE_LENGTH);
        for (int i = 0; i < SHORT_CODE_LENGTH; i++) {
            builder.append(CHARACTERS.charAt(secureRandom.nextInt(CHARACTERS.length())));
        }
        return builder.toString();
    }

    private UrlResponse toResponse(UrlEntity url) {
        return new UrlResponse(
                url.getId(),
                url.getLongUrl(),
                url.getShortCode(),
                baseUrl + url.getShortCode(),
                url.getCreatedAt());
    }
}
