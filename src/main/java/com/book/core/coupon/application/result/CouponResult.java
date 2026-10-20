package com.book.core.coupon.application.result;

import com.book.core.coupon.domain.Coupon;
import com.book.core.coupon.domain.enums.CouponType;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CouponResult(Long id, String name, CouponType type, BigDecimal discount, BigDecimal minOrderAmount,
    BigDecimal maxDiscountAmount, LocalDateTime expiredAt) {
    public CouponResult(final Long id, final String name, final CouponType type, final BigDecimal discount, final BigDecimal minOrderAmount,
        final LocalDateTime expiredAt) {
        this(id, name, type, discount, minOrderAmount, null, expiredAt);
    }

    public static CouponResult from(final Coupon coupon) {
        return new CouponResult(coupon.id(), coupon.name(), coupon.type(), coupon.discount(), coupon.minOrderAmount(),
            coupon.maxDiscountAmount(), coupon.expiredAt());
    }
}
