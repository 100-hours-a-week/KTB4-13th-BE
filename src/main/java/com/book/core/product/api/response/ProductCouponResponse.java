package com.book.core.product.api.response;

import com.book.core.product.application.result.GetProductCouponResult;
import java.math.BigDecimal;

public record ProductCouponResponse(
        Long id,
        String status,
        String name,
        String type,
        BigDecimal discount,
        BigDecimal minOrderAmount,
        BigDecimal maxDiscountAmount,
        Integer maxUseCount,
        Integer usedCount,
        String expiredAt) {
    public static ProductCouponResponse from(final GetProductCouponResult result) {
        return new ProductCouponResponse(
                result.id(),
                result.status(),
                result.name(),
                result.type(),
                result.discount(),
                result.minOrderAmount(),
                result.maxDiscountAmount(),
                result.maxUseCount(),
                result.usedCount(),
                result.expiredAt());
    }
}
