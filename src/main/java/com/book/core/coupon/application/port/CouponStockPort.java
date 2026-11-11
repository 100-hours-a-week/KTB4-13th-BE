package com.book.core.coupon.application.port;

public interface CouponStockPort {
    void initialize(final Long couponId, final int quantity);

    IssueResult tryIssue(final Long couponId);

    void markSoldOut(long couponId);

    boolean hasSoldOutSignal(final Long couponId);

    CouponStockSnapshot readSnapshot(final long couponId);
}
