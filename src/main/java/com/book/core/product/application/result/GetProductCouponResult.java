package com.book.core.product.application.result;

import com.book.core.coupon.application.result.CouponResult;
import java.math.BigDecimal;

public record GetProductCouponResult(Long id, String status, String name, String type, BigDecimal discount, BigDecimal minOrderAmount,
    BigDecimal maxDiscountAmount, Integer maxUseCount, Integer usedCount, String expiredAt) {
    public static GetProductCouponResult from(final CouponResult coupon) {
        return new GetProductCouponResult(coupon.id(), "ACTIVE", coupon.name(), coupon.type().name(), coupon.discount(),
            coupon.minOrderAmount(), null, null, null, coupon.expiredAt().toString());
    }
}
