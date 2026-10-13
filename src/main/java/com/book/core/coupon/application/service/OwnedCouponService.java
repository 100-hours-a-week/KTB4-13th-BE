package com.book.core.coupon.application.service;

import com.book.core.coupon.application.command.IssueCouponCommand;
import com.book.core.coupon.application.usecase.IssueCouponUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;


@Service
@RequiredArgsConstructor
public class OwnedCouponService {
    private final IssueCouponUseCase issueCouponUseCase;



    public void issue(final IssueCouponCommand command) {
        issueCouponUseCase.execute(command.userId(), command.couponId());
    }


}
