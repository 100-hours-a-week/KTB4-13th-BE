package com.book.core.coupon.infrastructure.cache;

import java.util.Set;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties("coupon.waiting-room")
record CouponWaitingRoomProperties(@DefaultValue Set<Long> couponIds, @DefaultValue("250") int admissionsPerSecond,
    @DefaultValue("30000") long passTtlMs, @DefaultValue("5") int requestsPerSecond, @DefaultValue("10") int requestBurst) {
    CouponWaitingRoomProperties {
        couponIds = Set.copyOf(couponIds);
        if (admissionsPerSecond <= 0 || passTtlMs <= 0 || requestsPerSecond <= 0 || requestBurst <= 0
            || couponIds.stream().anyMatch((final Long id) -> id <= 0)) {
            throw new IllegalArgumentException("쿠폰 대기열 설정은 양수여야 합니다.");
        }
    }
}
