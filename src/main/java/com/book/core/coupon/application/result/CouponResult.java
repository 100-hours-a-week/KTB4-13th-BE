package com.book.core.coupon.application.result;

import com.book.core.coupon.domain.Coupon;
import com.book.core.coupon.domain.enums.CouponType;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CouponResult(Long id, String name, CouponType type, BigDecimal discount, BigDecimal minOrderAmount, LocalDateTime expiredAt) {
    public static CouponResult from(final Coupon coupon) {
        return new CouponResult(coupon.id(), coupon.name(), coupon.type(), coupon.discount(), coupon.minOrderAmount(), coupon.expiredAt());
    }
}
