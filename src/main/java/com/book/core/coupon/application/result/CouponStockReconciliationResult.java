package com.book.core.coupon.application.result;

import java.time.Instant;

public record CouponStockReconciliationResult(long couponId, Instant observedAt, int totalQuantity, int issuedQuantity,
    long ownedCouponCount, Integer redisStock, boolean soldOutSignalPresent, long pendingReservations) {
    public long expectedStock() {
        return (long) totalQuantity - issuedQuantity - pendingReservations;
    }

    public Long stockDifference() {
        return redisStock == null ? null : expectedStock() - redisStock;
    }

    public boolean hasIssuanceCountMismatch() {
        return issuedQuantity != ownedCouponCount;
    }

    public boolean hasUnexpectedSoldOutSignal() {
        return soldOutSignalPresent && expectedStock() > 0;
    }

    public boolean isConsistentlySoldOut() {
        return expectedStock() == 0 && Integer.valueOf(0).equals(redisStock) && !hasIssuanceCountMismatch();
    }
}
