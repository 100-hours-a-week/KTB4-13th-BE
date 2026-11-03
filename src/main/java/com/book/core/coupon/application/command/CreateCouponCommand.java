package com.book.core.coupon.application.command;

import com.book.core.coupon.domain.enums.CouponType;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CreateCouponCommand(String name, CouponType type, BigDecimal discount, long maxUseCount, BigDecimal minOrderAmount,
    int totalQuantity, LocalDateTime startsAt, LocalDateTime expiredAt) {
}
