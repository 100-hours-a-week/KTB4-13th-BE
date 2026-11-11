package com.book.core.coupon.application.port;

public interface CouponWaitingRoomPort {
    boolean isEnabled(final long couponId);

    int admissionsPerSecond();

    boolean allowIssueRequest(long couponId, long userId);

    long enter(final long couponId, final long userId);

    long position(final long couponId, final long userId);
}
