package com.book.core.coupon.infrastructure.cache;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import java.time.Duration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties({CacheProperties.class, SoldOutProperties.class})
public class CacheConfig {
    private static final long MAX_TRACKED_COUPONS = 1_000L;

    @Bean
    LoadingCache<String, Boolean> couponSoldOutCache(final StringRedisTemplate redisTemplate, final SoldOutProperties soldOutProperties) {
        return Caffeine.newBuilder().expireAfterWrite(Duration.ofMillis(soldOutProperties.fastPathTtlMs())).maximumSize(MAX_TRACKED_COUPONS)
            .build((final String key) -> Boolean.TRUE.equals(redisTemplate.hasKey(key)));
    }
}
