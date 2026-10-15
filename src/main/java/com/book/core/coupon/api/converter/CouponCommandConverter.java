package com.book.core.coupon.api.converter;

import com.book.core.coupon.application.command.IssueCouponCommand;
import org.springframework.stereotype.Component;

@Component
public class CouponCommandConverter {
    public IssueCouponCommand toIssueCouponCommand(final Long userId, final Long couponId) {
        return new IssueCouponCommand(userId, couponId);
    }


}
