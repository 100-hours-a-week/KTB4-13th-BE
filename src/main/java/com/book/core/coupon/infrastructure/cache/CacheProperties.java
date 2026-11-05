package com.book.core.coupon.infrastructure.cache;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "coupon.cache")
public record CacheProperties(long ttlMs, @DefaultValue("0") long simulatedLoadLatencyMs) {
}
