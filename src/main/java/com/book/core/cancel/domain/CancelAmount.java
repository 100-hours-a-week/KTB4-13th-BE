package com.book.core.cancel.domain;

import java.math.BigDecimal;

/** 참고 CancelAmount의 취소 금액 배분과 최소 주문 금액 경계 조건을 유지합니다. */
public record CancelAmount(BigDecimal cancelAmount, BigDecimal totalOrderAmount, BigDecimal totalCanceledAmount,
    BigDecimal cancellablePaidAmount, BigDecimal cancellablePointAmount, BigDecimal cancellableCouponAmount, BigDecimal minOrderAmount) {
    /** 취소액에서 쿠폰 회수분을 먼저 빼고 남은 실결제 잔액 한도까지 배분합니다. */
    public BigDecimal paidAmount() {
        return cancelAmount.subtract(couponAmount()).min(cancellablePaidAmount);
    }

    /** 이번 취소로 최소 주문 금액 조건이 깨지면 취소액과 남은 쿠폰 금액 중 작은 값을 회수합니다. */
    public BigDecimal couponAmount() {
        if (isBrokenCoupon()) {
            return cancelAmount.min(cancellableCouponAmount);
        }
        return BigDecimal.ZERO;
    }

    /** 쿠폰과 실결제 배분 뒤 남은 금액을 사용 포인트 잔액 한도까지 반환합니다. */
    public BigDecimal pointAmount() {
        return cancelAmount.subtract(couponAmount()).subtract(paidAmount()).min(cancellablePointAmount);
    }

    private BigDecimal remainOrderAmount() {
        return totalOrderAmount.subtract(totalCanceledAmount);
    }

    /** 취소 전 잔액이 최소 주문 금액보다 엄격히 크고, 이번 취소 뒤 미달할 때만 쿠폰을 복원합니다. 같을 때는 복원하지 않습니다. */
    public boolean isRestoreCoupon() {
        return remainOrderAmount().compareTo(minOrderAmount) > 0 && isBrokenCoupon();
    }

    private boolean isBrokenCoupon() {
        return remainOrderAmount().subtract(cancelAmount).compareTo(minOrderAmount) < 0;
    }
}
