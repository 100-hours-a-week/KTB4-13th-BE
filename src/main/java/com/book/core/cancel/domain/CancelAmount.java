package com.book.core.cancel.domain;

import java.math.BigDecimal;

public record CancelAmount(BigDecimal cancelAmount, BigDecimal totalOrderAmount, BigDecimal totalCanceledAmount,
    BigDecimal cancellablePaidAmount, BigDecimal cancellablePointAmount, BigDecimal cancellableCouponAmount, BigDecimal minOrderAmount) {

    public BigDecimal paidAmount() {
        return cancelAmount.subtract(couponAmount()).min(cancellablePaidAmount);
    }

    public BigDecimal couponAmount() {
        if (isBrokenCoupon()) {
            return cancelAmount.min(cancellableCouponAmount);
        }
        return BigDecimal.ZERO;
    }

    public BigDecimal pointAmount() {
        return cancelAmount.subtract(couponAmount()).subtract(paidAmount()).min(cancellablePointAmount);
    }

    private BigDecimal remainOrderAmount() {
        return totalOrderAmount.subtract(totalCanceledAmount);
    }

    public boolean isRestoreCoupon() {
        return remainOrderAmount().compareTo(minOrderAmount) > 0 && isBrokenCoupon();
    }

    private boolean isBrokenCoupon() {
        return remainOrderAmount().subtract(cancelAmount).compareTo(minOrderAmount) < 0;
    }
}
