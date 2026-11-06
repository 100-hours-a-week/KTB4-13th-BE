package com.book.core.coupon.infrastructure.cache;

import com.book.core.coupon.application.port.CouponCacheRepositoryPort;
import com.book.core.coupon.application.service.CacheMetrics;
import com.book.core.coupon.domain.CouponIssuePolicy;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
@RequiredArgsConstructor
public class CouponIssuePolicyCacheAdapter implements CouponCacheRepositoryPort {
    private static final long LOCK_TTL_MILLIS = 3_000L;
    private static final int MAX_ATTEMPTS = 50;
    private static final long POLL_INTERVAL_MILLIS = 20L;
    @SuppressWarnings("rawtypes")
    private static final RedisScript<List> GET_OR_LOCK = RedisLuaSupport.listLuaScript("lua/coupon-policy-get-or-lock.lua");
    private static final RedisScript<Long> COMPLETE_LOAD = RedisLuaSupport.longLuaScript("lua/coupon-policy-complete-load.lua");

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final CacheProperties cacheProperties;
    private final CacheMetrics cacheMetrics;

    @Override
    public CouponIssuePolicy getOrLoad(final Long couponId, final Supplier<CouponIssuePolicy> loader) {
        final String cacheKey = "coupon:" + couponId + ":issue-policy";
        final List<String> keys = List.of(cacheKey, cacheKey + ":lock");
        final String token = UUID.randomUUID().toString();

        for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
            final List<String> result = RedisLuaSupport.runForStrings(redisTemplate, GET_OR_LOCK, keys, token, LOCK_TTL_MILLIS);
            switch (result.getFirst()) {
                case "HIT" -> {
                    final CouponIssuePolicy policy = objectMapper.readValue(result.get(1), CouponIssuePolicy.class);
                    cacheMetrics.incrementCouponCacheHit();
                    return policy;
                }
                case "LOAD" -> {
                    return loadAndCache(keys, token, loader);
                }
                case "WAIT" -> waitForRetry();
            }
        }

        throw new IllegalStateException("쿠폰 발급 정책 캐시 로딩 대기 시간 초과. couponId=" + couponId);
    }

    private CouponIssuePolicy loadAndCache(final List<String> keys, final String token, final Supplier<CouponIssuePolicy> loader) {
        final long ttlMs = cacheProperties.ttlMs();
        boolean completed = false;
        try {
            final CouponIssuePolicy policy = loader.get();
            final String json = objectMapper.writeValueAsString(policy);
            RedisLuaSupport.runForLong(redisTemplate, COMPLETE_LOAD, keys, token, json, ttlMs);
            completed = true;
            return policy;
        } finally {
            if (!completed) {
                RedisLuaSupport.runForLong(redisTemplate, COMPLETE_LOAD, keys, token);
            }
        }
    }

    private void waitForRetry() {
        try {
            Thread.sleep(POLL_INTERVAL_MILLIS);
        } catch (final InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("쿠폰 발급 정책 캐시 로딩 대기 중 인터럽트", exception);
        }
    }
}
