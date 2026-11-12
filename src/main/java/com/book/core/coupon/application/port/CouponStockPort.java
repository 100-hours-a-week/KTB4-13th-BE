package com.book.core.coupon.application.port;

import java.util.Set;

public interface CouponStockPort {
    void initialize(final Long couponId, final int quantity);

    boolean hasSoldOutSignal(final Long couponId);

    CouponStockSnapshot readSnapshot(final long couponId);

    boolean repairSnapshot(long couponId, CouponStockSnapshot observed, Set<Long> missingUserIds);
}
