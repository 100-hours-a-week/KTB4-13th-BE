package com.book.core.payment.domain;

import com.book.core.coupon.domain.enums.CouponType;
import java.math.BigDecimal;

public record PaymentCoupon(Long orderItemId, Long id, Long couponId, CouponType type, BigDecimal discount, BigDecimal minOrderAmount,
    BigDecimal maxDiscountAmount) {
    public PaymentCoupon(final Long id, final CouponType type, final BigDecimal discount, final BigDecimal minOrderAmount) {
        this(null, id, id, type, discount, minOrderAmount, null);
    }
}
