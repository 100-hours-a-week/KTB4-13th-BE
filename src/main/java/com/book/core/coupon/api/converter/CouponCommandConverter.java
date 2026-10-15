package com.book.core.coupon.api.converter;

import com.book.core.coupon.application.command.IssueCouponCommand;
import com.book.core.coupon.application.command.GetOwnedCouponsCommand;
import org.springframework.stereotype.Component;

@Component
public class CouponCommandConverter {
    public IssueCouponCommand toIssueCouponCommand(final Long userId, final Long couponId) {
        return new IssueCouponCommand(userId, couponId);
    }

    public GetOwnedCouponsCommand toGetOwnedCouponsCommand(final Long userId) {
        return new GetOwnedCouponsCommand(userId);
    }
}
