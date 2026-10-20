package com.book.core.payment.domain;

import com.book.core.coupon.domain.enums.CouponType;
import java.math.BigDecimal;

public record OrderItemPaymentAllocation(Long orderItemId, Long ownedCouponId, CouponType couponType, BigDecimal couponDiscountValue,
    BigDecimal couponMinOrderAmount, BigDecimal couponMaxDiscountAmount, BigDecimal couponDiscountAmount, BigDecimal usedPointAmount) {
}
