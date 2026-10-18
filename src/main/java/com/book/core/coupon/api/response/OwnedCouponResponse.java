package com.book.core.coupon.api.response;

import com.book.core.coupon.domain.enums.CouponType;
import com.book.core.coupon.domain.enums.OwnedCouponStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record OwnedCouponResponse(Long id, OwnedCouponStatus state, String name, CouponType type, BigDecimal discount,
    BigDecimal minOrderAmount, long maxUseCount, long usedCount, LocalDateTime expiredAt) {
}
