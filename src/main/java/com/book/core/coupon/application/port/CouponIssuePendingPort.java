package com.book.core.coupon.application.port;

public interface CouponIssuePendingPort {
    long countPending(long couponId);
}
