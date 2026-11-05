package com.book.core.coupon.application.result;

public record CacheMetricsSnapshot(long couponDbReads, long couponCacheHits, long soldOutRedisExists, long soldOutFastPathHits) {
}
