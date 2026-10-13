package com.book.core.coupon.application.service;

import com.book.core.coupon.application.result.CouponResult;
import com.book.core.coupon.application.usecase.GetCouponTargetsUseCase;
import com.book.core.coupon.application.usecase.GetCouponsUseCase;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CouponService {
    private final GetCouponTargetsUseCase couponTargetReader;
    private final GetCouponsUseCase couponReader;

    public List<CouponResult> getCouponsForProducts(final List<Long> productIds) {
        final var couponIds = couponTargetReader.execute(productIds);
        return couponReader.execute(couponIds);
    }
}
