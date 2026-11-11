package com.book.core.coupon.application.result;

import java.time.Instant;

public record CouponStockReconciliationResult(long couponId, Instant observedAt, int totalQuantity, int issuedQuantity,
    long ownedCouponCount, Integer redisStock, boolean soldOutSignalPresent, long pendingReservations, boolean stableSnapshot) {
    public long expectedStock() {
        return (long) totalQuantity - issuedQuantity - pendingReservations;
    }

    public Long stockDifference() {
        return !stableSnapshot || redisStock == null ? null : expectedStock() - redisStock;
    }

    public boolean hasIssuanceCountMismatch() {
        return issuedQuantity != ownedCouponCount;
    }

    public boolean hasUnexpectedSoldOutSignal() {
        return stableSnapshot && soldOutSignalPresent && expectedStock() > 0;
    }

    public boolean isConsistentlySoldOut() {
        return outcome() == CouponReconcileOutcome.MATCHED && Integer.valueOf(0).equals(redisStock);
    }

    public CouponReconcileOutcome outcome() {
        if (!stableSnapshot) {
            return CouponReconcileOutcome.SNAPSHOT_CHANGED;
        }
        if (redisStock == null) {
            return CouponReconcileOutcome.REDIS_STOCK_MISSING;
        }
        if (redisStock < 0 || stockDifference() != 0 || hasIssuanceCountMismatch() || hasUnexpectedSoldOutSignal()) {
            return CouponReconcileOutcome.DRIFT;
        }
        if (pendingReservations > 0) {
            return CouponReconcileOutcome.PENDING;
        }
        return CouponReconcileOutcome.MATCHED;
    }
}
