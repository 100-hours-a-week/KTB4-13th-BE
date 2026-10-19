package com.book.core.coupon.application.service;

import com.book.core.coupon.application.command.IssueCouponCommand;
import com.book.core.coupon.application.command.GetOwnedCouponsCommand;
import com.book.core.coupon.application.result.OwnedCouponResult;
import com.book.core.coupon.application.usecase.IssueCouponUseCase;
import com.book.core.coupon.application.usecase.FindOwnedCouponsForCheckoutUseCase;
import com.book.core.coupon.application.usecase.GetCouponTargetsUseCase;
import com.book.core.coupon.application.usecase.GetOwnedCouponsUseCase;
import java.util.Collection;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import static java.time.LocalDateTime.now;
import static java.util.Collections.emptyList;

@Service
@RequiredArgsConstructor
public class OwnedCouponService {
    private final GetOwnedCouponsUseCase getOwnedCouponsUseCase;
    private final IssueCouponUseCase issueCouponUseCase;
    private final GetCouponTargetsUseCase getCouponTargetsUseCase;
    private final FindOwnedCouponsForCheckoutUseCase findOwnedCouponsForCheckoutUseCase;

    public List<OwnedCouponResult> getOwnedCoupons(final GetOwnedCouponsCommand command) {
        return getOwnedCouponsUseCase.execute(command.userId());
    }

    public void issue(final IssueCouponCommand command) {
        issueCouponUseCase.execute(command.userId(), command.couponId());
    }

    public List<OwnedCouponResult> getOwnedCouponsForCheckout(final Long userId, final Collection<Long> productIds) {
        if (productIds.isEmpty()) {
            return emptyList();
        }
        final var applicableCouponIds = getCouponTargetsUseCase.execute(productIds);
        if (applicableCouponIds.isEmpty()) {
            return emptyList();
        }

        return findOwnedCouponsForCheckoutUseCase.execute(userId, applicableCouponIds, now());
    }
}
