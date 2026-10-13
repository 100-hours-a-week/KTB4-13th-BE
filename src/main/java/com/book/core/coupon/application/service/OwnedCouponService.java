package com.book.core.coupon.application.service;

import com.book.core.coupon.application.command.IssueCouponCommand;
import com.book.core.coupon.application.command.GetOwnedCouponsCommand;
import com.book.core.coupon.application.result.OwnedCouponResult;
import com.book.core.coupon.application.usecase.IssueCouponUseCase;
import com.book.core.coupon.application.usecase.GetOwnedCouponsUseCase;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;


@Service
@RequiredArgsConstructor
public class OwnedCouponService {
    private final GetOwnedCouponsUseCase getOwnedCouponsUseCase;
    private final IssueCouponUseCase issueCouponUseCase;

    public List<OwnedCouponResult> getOwnedCoupons(final GetOwnedCouponsCommand command) {
        return getOwnedCouponsUseCase.execute(command.userId());
    }

    public void issue(final IssueCouponCommand command) {
        issueCouponUseCase.execute(command.userId(), command.couponId());
    }


}
