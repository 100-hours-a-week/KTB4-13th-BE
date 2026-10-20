package com.book.core.coupon.api.response;

import com.book.core.coupon.domain.enums.CouponType;
import com.book.core.coupon.domain.enums.OwnedCouponStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record OwnedCouponResponse(Long id, OwnedCouponStatus state, String name, CouponType type, BigDecimal discount,
    BigDecimal minOrderAmount, BigDecimal maxDiscountAmount, long maxUseCount, long usedCount, LocalDateTime expiredAt) {
    public OwnedCouponResponse(final Long id, final OwnedCouponStatus state, final String name, final CouponType type,
        final BigDecimal discount, final BigDecimal minOrderAmount, final long maxUseCount, final long usedCount,
        final LocalDateTime expiredAt) {
        this(id, state, name, type, discount, minOrderAmount, null, maxUseCount, usedCount, expiredAt);
    }
}
