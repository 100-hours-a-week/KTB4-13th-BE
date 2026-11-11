package com.book.core.coupon.application.service;

import com.book.core.coupon.application.command.CreateCouponCommand;
import com.book.core.coupon.application.command.GetCouponsCommand;
import com.book.core.coupon.application.command.IssueCouponCommand;
import com.book.core.coupon.application.result.CouponIssueState;
import com.book.core.coupon.application.result.CouponResult;
import com.book.core.coupon.application.usecase.CreateCouponUseCase;
import com.book.core.coupon.application.usecase.EnqueueCouponIssueUseCase;
import com.book.core.coupon.application.usecase.GetCouponIssuePolicyUseCase;
import com.book.core.coupon.application.usecase.GetCouponTargetsUseCase;
import com.book.core.coupon.application.usecase.GetCouponsUseCase;
import com.book.core.coupon.application.usecase.IssueCouponUseCase;
import java.util.List;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CouponService {
    private final GetCouponTargetsUseCase couponTargetReader;
    private final GetCouponsUseCase couponReader;
    private final CreateCouponUseCase couponCreator;
    private final IssueCouponUseCase issueCouponUseCase;
    private final GetCouponIssuePolicyUseCase getCouponIssuePolicyUseCase;

    private final EnqueueCouponIssueUseCase enqueueCouponIssueUseCase;

    public CouponResult createCoupon(final CreateCouponCommand command) {
        return couponCreator.execute(command);
    }

    public void issue(final IssueCouponCommand command) {
        issueCouponUseCase.rejectIfSoldOut(command.couponId());
        final CouponIssueState issueState = getCouponIssuePolicyUseCase.execute(command.couponId());
        enqueueCouponIssueUseCase.execute(command, issueState);
    }

    public List<CouponResult> getCoupons(final GetCouponsCommand command) {
        return couponReader.execute(command.couponIds());
    }

    public List<CouponResult> getCouponsForProducts(final List<Long> productIds) {
        final var couponIds = couponTargetReader.execute(productIds);
        return couponReader.execute(couponIds);
    }
}
