package com.book.core.coupon.infrastructure.cache;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "coupon.cache")
public record CacheProperties(long ttlMs, long freshMs, long simulatedLoadLatencyMs) {
}
