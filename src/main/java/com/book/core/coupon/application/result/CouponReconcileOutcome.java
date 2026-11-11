package com.book.core.coupon.application.result;

public enum CouponReconcileOutcome {
    MATCHED,
    PENDING,
    DRIFT,
    REDIS_STOCK_MISSING,
    SNAPSHOT_CHANGED
}
