package com.book.core.product.application.result;

import java.math.BigDecimal;

public record GetProductCouponResult(
        Long id,
        String status,
        String name,
        String type,
        BigDecimal discount,
        BigDecimal minOrderAmount,
        BigDecimal maxDiscountAmount,
        Integer maxUseCount,
        Integer usedCount,
        String expiredAt) {}
