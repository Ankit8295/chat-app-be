package com.thechat.urlshortener;

import java.util.Optional;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class UrlCacheStore {

    private static final String KEY_PREFIX = "url:";

    private final StringRedisTemplate stringRedisTemplate;

    public UrlCacheStore(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    public Optional<String> get(String shortCode) {
        String value = stringRedisTemplate.opsForValue().get(key(shortCode));
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }
        return Optional.of(value);
    }

    public void put(String shortCode, String longUrl) {
        stringRedisTemplate.opsForValue().set(key(shortCode), longUrl);
    }

    public void delete(String shortCode) {
        stringRedisTemplate.delete(key(shortCode));
    }

    private static String key(String shortCode) {
        return KEY_PREFIX + shortCode;
    }
}
