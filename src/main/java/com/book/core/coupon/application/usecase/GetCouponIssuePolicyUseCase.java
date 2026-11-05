package com.book.core.coupon.application.usecase;

import com.book.common.annotation.UseCase;
import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import com.book.core.coupon.application.port.CouponCacheRepositoryPort;
import com.book.core.coupon.application.port.CouponLoadSimulationPort;
import com.book.core.coupon.application.port.CouponRepositoryPort;
import com.book.core.coupon.application.service.CacheMetrics;
import com.book.core.coupon.domain.CouponIssuePolicy;
import lombok.RequiredArgsConstructor;

import java.util.Optional;

@UseCase
@RequiredArgsConstructor
public class GetCouponIssuePolicyUseCase {
    private final CouponRepositoryPort couponRepository;
    private final CacheMetrics cacheMetrics;
    private final CouponLoadSimulationPort couponLoadSimulationPort;
    private final CouponCacheRepositoryPort couponCacheRepository;

    public CouponIssuePolicy execute(final Long couponId) {
        final Optional<CouponIssuePolicy> cachedCouponIssuePolicy = couponCacheRepository.find(couponId);
        if (cachedCouponIssuePolicy.isPresent()) {
            cacheMetrics.incrementCouponCacheHit();
            return cachedCouponIssuePolicy.get();
        }

        couponLoadSimulationPort.simulateCouponReadLatency();
        cacheMetrics.incrementCouponDbRead();
        final CouponIssuePolicy policy = couponRepository.findById(couponId).map(CouponIssuePolicy::from)
            .orElseThrow(() -> new CoreException(ErrorCode.COUPON_NOT_FOUND_OR_EXPIRED));
        couponCacheRepository.save(couponId, policy);
        return policy;
    }
}
