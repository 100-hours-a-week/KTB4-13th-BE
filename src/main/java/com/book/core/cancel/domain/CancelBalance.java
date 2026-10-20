package com.book.core.cancel.domain;

import com.book.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

@Entity
@Table(name = "cancel_balance", indexes = @Index(name = "udx_cancel_balance_order_id", columnList = "order_id", unique = true))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Accessors(fluent = true)
public class CancelBalance extends BaseEntity {
    @Column(name = "order_id", nullable = false)
    private Long orderId;
    @Column(name = "cancellable_paid_amount", nullable = false, precision = 38, scale = 2)
    private BigDecimal cancellablePaidAmount;
    @Column(name = "cancellable_point_amount", nullable = false, precision = 38, scale = 2)
    private BigDecimal cancellablePointAmount;
    @Column(name = "cancellable_coupon_amount", nullable = false, precision = 38, scale = 2)
    private BigDecimal cancellableCouponAmount;
    @Column(name = "cancelled_paid_amount", nullable = false, precision = 38, scale = 2)
    private BigDecimal cancelledPaidAmount;
    @Column(name = "cancelled_point_amount", nullable = false, precision = 38, scale = 2)
    private BigDecimal cancelledPointAmount;
    @Column(name = "cancelled_coupon_amount", nullable = false, precision = 38, scale = 2)
    private BigDecimal cancelledCouponAmount;

    public CancelBalance(final Long orderId, final BigDecimal cancellablePaidAmount, final BigDecimal cancellablePointAmount,
        final BigDecimal cancellableCouponAmount, final BigDecimal cancelledPaidAmount, final BigDecimal cancelledPointAmount,
        final BigDecimal cancelledCouponAmount) {
        this.orderId = orderId;
        this.cancellablePaidAmount = cancellablePaidAmount;
        this.cancellablePointAmount = cancellablePointAmount;
        this.cancellableCouponAmount = cancellableCouponAmount;
        this.cancelledPaidAmount = cancelledPaidAmount;
        this.cancelledPointAmount = cancelledPointAmount;
        this.cancelledCouponAmount = cancelledCouponAmount;
    }

    @Version
    private long version = 0L;

    public void cancel(final BigDecimal paidAmount, final BigDecimal pointAmount, final BigDecimal couponAmount) {
        cancellablePaidAmount = cancellablePaidAmount.subtract(paidAmount);
        cancellablePointAmount = cancellablePointAmount.subtract(pointAmount);
        cancellableCouponAmount = cancellableCouponAmount.subtract(couponAmount);
        cancelledPaidAmount = cancelledPaidAmount.add(paidAmount);
        cancelledPointAmount = cancelledPointAmount.add(pointAmount);
        cancelledCouponAmount = cancelledCouponAmount.add(couponAmount);
    }

    public BigDecimal totalCanceledAmount() {
        return cancelledPaidAmount.add(cancelledPointAmount).add(cancelledCouponAmount);
    }
}
