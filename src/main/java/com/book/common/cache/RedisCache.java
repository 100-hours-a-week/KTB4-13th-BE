package com.book.common.cache;

import java.time.Duration;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
@RequiredArgsConstructor
public class RedisCache {
    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    public <T> Optional<T> get(final String key, final Class<T> valueType) {
        final String value = redisTemplate.opsForValue().get(key);
        return Optional.ofNullable(value).filter(cached -> !cached.isEmpty()).map(cached -> objectMapper.readValue(cached, valueType));
    }

    /** Stores a JSON value with a positive TTL in milliseconds. */
    public <T> void set(final String key, final T value, final long ttlMillis) {
        if (ttlMillis <= 0) {
            throw new IllegalArgumentException("Cache TTL must be positive");
        }
        final String serialized = objectMapper.writeValueAsString(value);
        redisTemplate.opsForValue().set(key, serialized, Duration.ofMillis(ttlMillis));
    }
}
