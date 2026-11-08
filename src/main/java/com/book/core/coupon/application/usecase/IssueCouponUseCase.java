package com.book.core.coupon.application.usecase;

import com.book.common.annotation.UseCase;
import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import com.book.core.coupon.application.port.CouponRepositoryPort;
import com.book.core.coupon.application.port.CouponStockPort;
import com.book.core.coupon.application.port.IssueResult;
import com.book.core.coupon.application.port.OwnedCouponRepositoryPort;
import com.book.core.coupon.application.result.CouponIssueState;
import com.book.core.coupon.application.result.CouponIssueState.Stock;
import com.book.core.coupon.application.service.CacheMetrics;
import com.book.core.coupon.domain.CouponIssuePolicy;
import com.book.core.coupon.domain.OwnedCoupon;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.transaction.annotation.Transactional;

import static java.time.LocalDateTime.now;

@UseCase
@RequiredArgsConstructor
@Slf4j
public class IssueCouponUseCase {
    private final CouponRepositoryPort couponRepository;
    private final OwnedCouponRepositoryPort ownedCouponRepository;
    private final CouponStockPort couponStockPort;
    private final CacheMetrics cacheMetrics;

    public void rejectIfSoldOut(final Long couponId) {
        if (couponStockPort.hasSoldOutSignal(couponId)) {
            cacheMetrics.incrementSoldOutFastPathHit();
            throw new CoreException(ErrorCode.COUPON_SOLD_OUT);
        }
    }

    @Transactional
    public void execute(final Long userId, final Long couponId, final CouponIssueState issueState) {
        log.atDebug().addKeyValue("couponId", couponId).log("[COUPON] 쿠폰 발급 처리를 시작합니다. couponId={}", couponId);

        final CouponIssuePolicy policy = resolveIssuablePolicy(couponId, issueState);
        if (policy.soldOut()) {
            throw new CoreException(ErrorCode.COUPON_SOLD_OUT);
        }
        if (ownedCouponRepository.existsByUserIdAndCouponId(userId, couponId)) {
            throw new CoreException(ErrorCode.COUPON_ALREADY_ISSUED);
        }

        tryIssue(couponId);
        if (couponRepository.incrementIssuedQuantity(couponId) == 0) {
            if (couponRepository.findById(couponId).isEmpty()) {
                throw new CoreException(ErrorCode.COUPON_NOT_FOUND_OR_EXPIRED);
            }
            throw new CoreException(ErrorCode.COUPON_SOLD_OUT);
        }

        final OwnedCoupon ownedCoupon = OwnedCoupon.of(userId, couponId, policy.maxUseCount());
        try {
            ownedCouponRepository.save(ownedCoupon);
        } catch (final Exception exception) {
            switch (exception) {
                case ConstraintViolationException e -> throw new CoreException(ErrorCode.COUPON_ALREADY_ISSUED, e);
                default -> throw exception;
            }
        }

        log.atInfo().addKeyValue("couponId", couponId).log("[COUPON] 쿠폰 발급 처리를 마쳤습니다. couponId={}", couponId);
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

    private void tryIssue(final Long couponId) {
        final IssueResult issueResult = couponStockPort.tryIssue(couponId);
        switch (issueResult) {
            case ISSUED -> log.atDebug().addKeyValue("couponId", couponId).log("[COUPON] 쿠폰 발급 성공. couponId={}", couponId);
            case SOLD_OUT -> {
                log.atDebug().addKeyValue("couponId", couponId).log("[COUPON] 쿠폰 발급 실패(재고 없음). couponId={}", couponId);
                throw new CoreException(ErrorCode.COUPON_SOLD_OUT);
            }
            case FAILED -> {
                log.atError().addKeyValue("couponId", couponId).log("[COUPON] 쿠폰 발급 실패(예상치 못한 결과). couponId={}", couponId);
                throw new CoreException(ErrorCode.COUPON_ISSUE_FAILED);
            }
        }
    }
}
