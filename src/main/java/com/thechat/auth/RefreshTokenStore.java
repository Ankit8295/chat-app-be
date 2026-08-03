package com.thechat.auth;

import java.time.Duration;
import java.util.Optional;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import tools.jackson.databind.json.JsonMapper;

@Component
public class RefreshTokenStore {

    private static final String KEY_PREFIX = "refresh:";

    private final StringRedisTemplate stringRedisTemplate;
    private final JsonMapper jsonMapper;

    public RefreshTokenStore(StringRedisTemplate stringRedisTemplate, JsonMapper jsonMapper) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.jsonMapper = jsonMapper;
    }

    public void save(String tokenHash, RefreshTokenSession session, Duration ttl) {
        String key = key(tokenHash);
        stringRedisTemplate.opsForValue().set(key, jsonMapper.writeValueAsString(session), ttl);
    }

    public Optional<RefreshTokenSession> find(String tokenHash) {
        String json = stringRedisTemplate.opsForValue().get(key(tokenHash));
        if (json == null || json.isBlank()) {
            return Optional.empty();
        }
        return Optional.of(jsonMapper.readValue(json, RefreshTokenSession.class));
    }

    public Optional<RefreshTokenSession> getAndDelete(String tokenHash) {
        String json = stringRedisTemplate.opsForValue().getAndDelete(key(tokenHash));
        if (json == null || json.isBlank()) {
            return Optional.empty();
        }
        return Optional.of(jsonMapper.readValue(json, RefreshTokenSession.class));
    }

    public void delete(String tokenHash) {
        stringRedisTemplate.delete(key(tokenHash));
    }

    private static String key(String tokenHash) {
        return KEY_PREFIX + tokenHash;
    }
}
