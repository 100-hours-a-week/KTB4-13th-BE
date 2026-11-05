package com.book.core.coupon.infrastructure.cache;

import com.book.common.cache.RedisCache;
import com.book.core.coupon.application.port.CouponCacheRepositoryPort;
import com.book.core.coupon.domain.CouponIssuePolicy;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class CouponIssuePolicyCacheAdapter implements CouponCacheRepositoryPort {
    private final RedisCache redisCache;
    private final CacheProperties cacheProperties;

    @Override
    public Optional<CouponIssuePolicy> find(final Long couponId) {
        return redisCache.get(key(couponId), CouponIssuePolicy.class);
    }

    @Override
    public void save(final Long couponId, final CouponIssuePolicy policy) {
        redisCache.set(key(couponId), policy, cacheProperties.ttlMs());
    }

    private String key(final Long couponId) {
        return "coupon:" + couponId + ":issue-policy";
    }
}
