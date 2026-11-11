package com.book.core.coupon.api.converter;

import com.book.core.coupon.api.request.CreateCouponRequest;
import com.book.core.coupon.application.command.CreateCouponCommand;
import com.book.core.coupon.application.command.GetCouponsCommand;
import com.book.core.coupon.application.command.GetOwnedCouponsCommand;
import com.book.core.coupon.application.command.IssueCouponCommand;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class CouponCommandConverter {
    public CreateCouponCommand toCreateCouponCommand(final CreateCouponRequest request) {
        return new CreateCouponCommand(request.name(), request.type(), request.discount(), request.maxUseCount(), request.minOrderAmount(),
            request.totalQuantity(), request.startsAt(), request.expiredAt());
    }

    public GetCouponsCommand toGetCouponsCommand(final List<Long> couponIds) {
        return new GetCouponsCommand(couponIds);
    }

    public IssueCouponCommand toIssueCouponCommand(final Long userId, final Long couponId) {
        return new IssueCouponCommand(userId, couponId);
    }

    public GetOwnedCouponsCommand toGetOwnedCouponsCommand(final Long userId) {
        return new GetOwnedCouponsCommand(userId);
    }
}
