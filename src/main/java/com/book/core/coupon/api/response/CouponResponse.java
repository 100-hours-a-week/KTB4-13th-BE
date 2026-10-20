package com.book.core.coupon.api.response;

import com.book.core.coupon.domain.enums.CouponType;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CouponResponse(Long id, String name, CouponType type, BigDecimal discount, BigDecimal minOrderAmount,
    BigDecimal maxDiscountAmount, LocalDateTime expiredAt) {
    public CouponResponse(final Long id, final String name, final CouponType type, final BigDecimal discount,
        final BigDecimal minOrderAmount, final LocalDateTime expiredAt) {
        this(id, name, type, discount, minOrderAmount, null, expiredAt);
    }
}
