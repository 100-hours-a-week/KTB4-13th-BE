package com.book.core.coupon.application.result;

import com.book.core.coupon.domain.Coupon;
import com.book.core.coupon.domain.OwnedCoupon;
import com.book.core.coupon.domain.enums.OwnedCouponStatus;

public record OwnedCouponResult(Long id, Long userId, OwnedCouponStatus state, long maxUseCount, long usedCount, CouponResult coupon) {
    public static OwnedCouponResult from(final OwnedCoupon ownedCoupon, final Coupon coupon) {
        return new OwnedCouponResult(ownedCoupon.id(), ownedCoupon.userId(), ownedCoupon.status(), ownedCoupon.maxUseCount(),
            ownedCoupon.usedCount(), CouponResult.from(coupon));
    }
}
