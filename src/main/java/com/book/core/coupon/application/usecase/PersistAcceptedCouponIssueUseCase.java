package com.book.core.coupon.application.usecase;

import com.book.common.annotation.UseCase;
import com.book.core.coupon.application.command.AcceptedCouponIssue;
import com.book.core.coupon.application.port.CouponRepositoryPort;
import com.book.core.coupon.application.port.OwnedCouponRepositoryPort;
import com.book.core.coupon.domain.OwnedCoupon;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@Profile({"coupon-streams & coupon-streams-worker", "coupon-kafka"})
@RequiredArgsConstructor
public class PersistAcceptedCouponIssueUseCase {
    private final CouponRepositoryPort coupons;
    private final OwnedCouponRepositoryPort ownedCoupons;

    @Transactional
    public boolean execute(final AcceptedCouponIssue issue) {
        if (ownedCoupons.existsByUserIdAndCouponId(issue.userId(), issue.couponId())) {
            return false;
        }
        if (coupons.incrementIssuedQuantity(issue.couponId()) != 1) {
            throw new IllegalStateException("접수된 쿠폰의 발급 수량을 반영할 수 없습니다.");
        }
        ownedCoupons.save(OwnedCoupon.of(issue.userId(), issue.couponId(), issue.maxUseCount()));
        return true;
    }
}
