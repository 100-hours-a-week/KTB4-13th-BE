package com.book.core.coupon.application.port;

public interface CouponStockPort {
    void initialize(final Long couponId, final int quantity);

    boolean hasSoldOutSignal(final Long couponId);

    CouponStockSnapshot readSnapshot(final long couponId);
}
