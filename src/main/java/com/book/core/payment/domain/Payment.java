package com.book.core.payment.domain;

import com.book.common.domain.BaseEntity;
import com.book.core.payment.domain.enums.PaymentMethod;
import com.book.core.payment.domain.enums.PaymentState;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

@Entity
@Table(name = "payment", indexes = @Index(name = "udx_payment_order_id", columnList = "order_id", unique = true))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Accessors(fluent = true)
public class Payment extends BaseEntity {
    @Column(name = "user_id", nullable = false)
    private Long userId;
    @Column(name = "order_id", nullable = false)
    private Long orderId;
    @Column(name = "origin_amount", nullable = false, precision = 38, scale = 2)
    private BigDecimal originAmount;
    @Column(name = "owned_coupon_id", nullable = false)
    private Long ownedCouponId;
    @Column(name = "coupon_discount", nullable = false, precision = 38, scale = 2)
    private BigDecimal couponDiscount;
    @Column(name = "used_point", nullable = false, precision = 38, scale = 2)
    private BigDecimal usedPoint;
    @Column(name = "payer_user_id", nullable = false)
    private Long payerUserId;
    @Column(name = "paid_amount", nullable = false, precision = 38, scale = 2)
    private BigDecimal paidAmount;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private PaymentState state;
    @Column(name = "external_payment_key")
    private String externalPaymentKey;
    @Enumerated(EnumType.STRING)
    @Column(length = 32)
    private PaymentMethod method;
    @Column(name = "approve_code")
    private String approveCode;
    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    public Payment(final Long userId, final Long orderId, final BigDecimal originAmount, final Long ownedCouponId,
        final BigDecimal couponDiscount, final BigDecimal usedPoint, final Long payerUserId, final BigDecimal paidAmount,
        final PaymentState state) {
        this.userId = userId;
        this.orderId = orderId;
        this.originAmount = originAmount;
        this.ownedCouponId = ownedCouponId;
        this.couponDiscount = couponDiscount;
        this.usedPoint = usedPoint;
        this.payerUserId = payerUserId;
        this.paidAmount = paidAmount;
        this.state = state;
    }

    public void success(final String externalPaymentKey, final PaymentMethod method, final String approveCode) {
        this.state = PaymentState.SUCCESS;
        this.externalPaymentKey = externalPaymentKey;
        this.method = method;
        this.approveCode = approveCode;
        this.paidAt = LocalDateTime.now();
    }

    public void prepareRetry(final Long payerUserId, final PaymentDiscount discount) {
        if (state != PaymentState.READY) {
            throw new IllegalStateException("완료된 결제는 다시 준비할 수 없습니다.");
        }
        this.payerUserId = payerUserId;
        this.ownedCouponId = discount.useOwnedCouponId();
        this.couponDiscount = discount.couponDiscount();
        this.usedPoint = discount.usePoint();
        this.paidAmount = discount.paidAmount(originAmount);
    }

    public boolean hasAppliedCoupon() {
        return ownedCouponId > 0;
    }
}
