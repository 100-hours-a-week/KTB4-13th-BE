package com.book.core.coupon.api.converter;

import com.book.core.coupon.api.response.OwnedCouponResponse;
import com.book.core.coupon.api.response.CouponResponse;
import com.book.core.coupon.application.result.CouponResult;
import com.book.core.coupon.application.result.OwnedCouponResult;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class CouponResultConverter {
    public List<CouponResponse> toCouponResponses(final List<CouponResult> coupons) {
        return coupons.stream().map((final var coupon) -> new CouponResponse(coupon.id(), coupon.name(), coupon.type(), coupon.discount(),
            coupon.minOrderAmount(), coupon.expiredAt())).toList();
    }

    public List<OwnedCouponResponse> toOwnedCouponResponses(final List<OwnedCouponResult> ownedCoupons) {
        return ownedCoupons.stream().map((final var ownedCoupon) -> {
            final var coupon = ownedCoupon.coupon();
            return new OwnedCouponResponse(ownedCoupon.id(), ownedCoupon.state(), coupon.name(), coupon.type(), coupon.discount(),
                coupon.minOrderAmount(), ownedCoupon.maxUseCount(), ownedCoupon.usedCount(), coupon.expiredAt());
        }).toList();
    }
}
