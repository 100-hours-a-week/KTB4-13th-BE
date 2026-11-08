package com.book.core.coupon.application.usecase;

import com.book.common.annotation.UseCase;
import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import com.book.core.coupon.application.port.CouponCacheRepositoryPort;
import com.book.core.coupon.application.port.CouponLoadSimulationPort;
import com.book.core.coupon.application.port.CouponRepositoryPort;
import com.book.core.coupon.application.result.CouponIssueState;
import com.book.core.coupon.application.service.CacheMetrics;
import com.book.core.coupon.domain.CouponIssuePolicy;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

@UseCase
@RequiredArgsConstructor
public class GetCouponIssuePolicyUseCase {
    private final CouponRepositoryPort couponRepository;
    private final CacheMetrics cacheMetrics;
    private final CouponLoadSimulationPort couponLoadSimulationPort;
    private final CouponCacheRepositoryPort couponCacheRepository;
    private final PlatformTransactionManager transactionManager;

    public CouponIssueState execute(final Long couponId) {
        return couponCacheRepository.getOrLoad(couponId, () -> loadPolicy(couponId));
    }

    private CouponIssuePolicy loadPolicy(final Long couponId) {
        couponLoadSimulationPort.simulateCouponReadLatency();
        cacheMetrics.incrementCouponDbRead();
        final TransactionTemplate policyReadTransaction = new TransactionTemplate(transactionManager);
        policyReadTransaction.setReadOnly(true);
        return policyReadTransaction.execute((final TransactionStatus status) -> couponRepository.findById(couponId)
            .map(CouponIssuePolicy::from).orElseThrow(() -> new CoreException(ErrorCode.COUPON_NOT_FOUND_OR_EXPIRED)));
    }
}
