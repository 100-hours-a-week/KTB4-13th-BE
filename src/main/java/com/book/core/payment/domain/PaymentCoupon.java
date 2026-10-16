package com.book.core.payment.domain;

import com.book.core.coupon.domain.enums.CouponType;
import java.math.BigDecimal;

public record PaymentCoupon(Long id, CouponType type, BigDecimal discount, BigDecimal minOrderAmount) {
}
