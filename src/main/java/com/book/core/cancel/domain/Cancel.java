package com.book.core.cancel.domain;

import com.book.common.domain.BaseEntity;
import com.book.core.cancel.domain.enums.CancelType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/** 참고 취소 내역의 원래 금액, 취소 금액, 수량, 유형 및 시각을 보관합니다. */
@Entity
@Table(name = "cancel")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Accessors(fluent = true)
public class Cancel extends BaseEntity {
    @Column(name = "user_id", nullable = false)
    private Long userId;
    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 32)
    private CancelType type;
    @Column(name = "order_id", nullable = false)
    private Long orderId;
    @Column(name = "order_item_id", nullable = false)
    private Long orderItemId;
    @Column(name = "payment_id", nullable = false)
    private Long paymentId;
    @Column(name = "origin_amount", nullable = false, precision = 38, scale = 2)
    private BigDecimal originAmount;
    @Column(name = "owned_coupon_id", nullable = false)
    private Long ownedCouponId;
    @Column(name = "coupon_discount", nullable = false, precision = 38, scale = 2)
    private BigDecimal couponDiscount;
    @Column(name = "used_point", nullable = false, precision = 38, scale = 2)
    private BigDecimal usedPoint;
    @Column(name = "paid_amount", nullable = false, precision = 38, scale = 2)
    private BigDecimal paidAmount;
    @Column(name = "canceled_quantity", nullable = false)
    private long canceledQuantity;
    @Column(name = "canceled_paid_amount", nullable = false, precision = 38, scale = 2)
    private BigDecimal canceledPaidAmount;
    @Column(name = "canceled_point_amount", nullable = false, precision = 38, scale = 2)
    private BigDecimal canceledPointAmount;
    @Column(name = "canceled_coupon_amount", nullable = false, precision = 38, scale = 2)
    private BigDecimal canceledCouponAmount;
    @Column(name = "external_cancel_key", nullable = false)
    private String externalCancelKey;
    @Column(name = "canceled_at", nullable = false)
    private LocalDateTime canceledAt;

    public Cancel(final Long userId, final CancelType type, final Long orderId, final Long orderItemId, final Long paymentId,
        final BigDecimal originAmount, final Long ownedCouponId, final BigDecimal couponDiscount, final BigDecimal usedPoint,
        final BigDecimal paidAmount, final long canceledQuantity, final BigDecimal canceledPaidAmount, final BigDecimal canceledPointAmount,
        final BigDecimal canceledCouponAmount, final String externalCancelKey, final LocalDateTime canceledAt) {
        this.userId = userId;
        this.type = type;
        this.orderId = orderId;
        this.orderItemId = orderItemId;
        this.paymentId = paymentId;
        this.originAmount = originAmount;
        this.ownedCouponId = ownedCouponId;
        this.couponDiscount = couponDiscount;
        this.usedPoint = usedPoint;
        this.paidAmount = paidAmount;
        this.canceledQuantity = canceledQuantity;
        this.canceledPaidAmount = canceledPaidAmount;
        this.canceledPointAmount = canceledPointAmount;
        this.canceledCouponAmount = canceledCouponAmount;
        this.externalCancelKey = externalCancelKey;
        this.canceledAt = canceledAt;
    }
}
