package com.book.core.coupon.application.usecase;

import com.book.common.annotation.UseCase;
import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import com.book.core.coupon.application.port.CouponRepositoryPort;
import com.book.core.coupon.application.port.CouponStockPort;
import com.book.core.coupon.application.port.OwnedCouponRepositoryPort;
import com.book.core.coupon.application.result.CouponIssueState;
import com.book.core.coupon.application.result.CouponIssueState.Stock;
import com.book.core.coupon.domain.CouponIssuePolicy;

import lombok.RequiredArgsConstructor;

import static java.time.LocalDateTime.now;

@UseCase
@RequiredArgsConstructor
public class IssueCouponUseCase {
    private final CouponRepositoryPort couponRepository;
    private final OwnedCouponRepositoryPort ownedCouponRepository;
    private final CouponStockPort couponStockPort;

    public void rejectIfSoldOut(final Long couponId) {
        if (couponStockPort.hasSoldOutSignal(couponId)) {
            throw new CoreException(ErrorCode.COUPON_SOLD_OUT);
        }
    }

    CouponIssuePolicy validateIssuable(final Long userId, final Long couponId, final CouponIssueState issueState) {
        final CouponIssuePolicy policy = resolveIssuablePolicy(couponId, issueState);
        if (policy.soldOut()) {
            throw new CoreException(ErrorCode.COUPON_SOLD_OUT);
        }
        if (ownedCouponRepository.existsByUserIdAndCouponId(userId, couponId)) {
            throw new CoreException(ErrorCode.COUPON_ALREADY_ISSUED);
        }

        return policy;
    }

    private CouponIssuePolicy resolveIssuablePolicy(final Long couponId, final CouponIssueState issueState) {
        final CouponIssuePolicy policy = issueState.policy();
        validateIssuePeriod(policy);
        if (!policy.soldOut() || issueState.stock() != Stock.AVAILABLE) {
            return policy;
        }

        // 복구된 Redis 재고와 매진 정책이 충돌할 때만 현재 DB 정책을 확인한다.
        final CouponIssuePolicy current = couponRepository.findById(couponId).map(CouponIssuePolicy::from)
            .orElseThrow(() -> new CoreException(ErrorCode.COUPON_NOT_FOUND_OR_EXPIRED));
        validateIssuePeriod(current);
        return current;
    }

    private void validateIssuePeriod(final CouponIssuePolicy policy) {
        if (!policy.isIssuableAt(now())) {
            throw new CoreException(ErrorCode.COUPON_NOT_FOUND_OR_EXPIRED);
        }
    }
}
