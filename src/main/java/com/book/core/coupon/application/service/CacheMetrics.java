package com.book.core.coupon.application.service;

import com.book.core.coupon.application.result.CacheMetricsSnapshot;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.stereotype.Service;

@Service
public class CacheMetrics {
    private final AtomicLong couponDbReads = new AtomicLong();
    private final AtomicLong couponCacheHits = new AtomicLong();
    private final AtomicLong soldOutRedisExists = new AtomicLong();
    private final AtomicLong soldOutFastPathHits = new AtomicLong();

    public void incrementCouponDbRead() {
        couponDbReads.incrementAndGet();
    }

    public void incrementCouponCacheHit() {
        couponCacheHits.incrementAndGet();
    }

    public void incrementSoldOutRedisExists() {
        soldOutRedisExists.incrementAndGet();
    }

    public void incrementSoldOutFastPathHit() {
        soldOutFastPathHits.incrementAndGet();
    }

    public CacheMetricsSnapshot snapshot() {
        return new CacheMetricsSnapshot(couponDbReads.get(), couponCacheHits.get(), soldOutRedisExists.get(), soldOutFastPathHits.get());
    }

    public void reset() {
        couponDbReads.set(0);
        couponCacheHits.set(0);
        soldOutRedisExists.set(0);
        soldOutFastPathHits.set(0);
    }
}
