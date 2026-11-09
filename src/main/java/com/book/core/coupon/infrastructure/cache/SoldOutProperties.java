package com.book.core.coupon.infrastructure.cache;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "coupon.sold-out")
public record SoldOutProperties(@Positive int ttlSeconds, @PositiveOrZero long fastPathTtlMs) {
}
