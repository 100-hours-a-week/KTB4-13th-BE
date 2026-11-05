package com.book.core.coupon.domain;

import java.time.LocalDateTime;

public record CouponIssuePolicy(LocalDateTime startsAt, LocalDateTime expiredAt, long maxUseCount, boolean soldOut) {
    public static CouponIssuePolicy from(final Coupon coupon) {
        return new CouponIssuePolicy(coupon.startsAt(), coupon.expiredAt(), coupon.maxUseCount(), coupon.isSoldOut());
    }

    public boolean isIssuableAt(final LocalDateTime now) {
        return !now.isBefore(startsAt) && now.isBefore(expiredAt);
    }
}
