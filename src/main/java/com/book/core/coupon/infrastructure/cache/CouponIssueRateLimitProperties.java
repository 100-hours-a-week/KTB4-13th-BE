package com.book.core.coupon.infrastructure.cache;

import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "coupon.issue-rate-limit")
public record CouponIssueRateLimitProperties(@Positive int maxRequests, @Positive int windowSeconds) {
}
