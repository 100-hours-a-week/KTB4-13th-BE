package com.book.core.coupon.api.response;

import com.book.core.coupon.application.result.CacheMetricsSnapshot;

public record CacheMetricsResponse(long couponDbReads, long couponCacheHits, long soldOutRedisExists, long soldOutFastPathHits) {
    public static CacheMetricsResponse from(final CacheMetricsSnapshot snapshot) {
        return new CacheMetricsResponse(snapshot.couponDbReads(), snapshot.couponCacheHits(), snapshot.soldOutRedisExists(),
            snapshot.soldOutFastPathHits());
    }
}
