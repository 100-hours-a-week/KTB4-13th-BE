package com.book.core.coupon.api.response;

import com.book.core.coupon.domain.enums.CouponType;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CouponResponse(Long id, String name, CouponType type, BigDecimal discount, BigDecimal minOrderAmount,
    LocalDateTime expiredAt) {
}
