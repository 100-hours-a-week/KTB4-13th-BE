package com.book.core.payment.domain;

import com.book.core.coupon.domain.enums.CouponType;
import java.math.BigDecimal;

/** 할인 계산에 필요한 보유 쿠폰의 업무 값입니다. */
public record PaymentCoupon(Long id, CouponType type, BigDecimal discount, BigDecimal minOrderAmount) {
}
