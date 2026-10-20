package com.book.core.coupon.application.usecase;

import com.book.common.annotation.UseCase;
import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import com.book.core.coupon.application.port.CouponRepositoryPort;
import com.book.core.coupon.application.port.OwnedCouponRepositoryPort;
import com.book.core.coupon.application.port.SignupCouponQuotaRepositoryPort;
import com.book.core.coupon.domain.Coupon;
import com.book.core.coupon.domain.OwnedCoupon;
import com.book.core.coupon.domain.SignupCouponQuota;
import com.book.core.coupon.domain.enums.OwnedCouponStatus;
import java.time.LocalDateTime;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor(onConstructor_ = @Autowired)
@Slf4j
public class IssueCouponUseCase {
    private final CouponRepositoryPort couponRepository;
    private final OwnedCouponRepositoryPort ownedCouponRepository;
    private final SignupCouponQuotaRepositoryPort signupCouponQuotaRepository;

    public IssueCouponUseCase(final CouponRepositoryPort couponRepository, final OwnedCouponRepositoryPort ownedCouponRepository) {
        this(couponRepository, ownedCouponRepository, couponId -> Optional.empty());
    }

    @Transactional
    public void execute(final Long userId, final Long couponId) {
        log.debug("쿠폰 발급 처리를 시작합니다. couponId={}, traceId={}", couponId, MDC.get("traceId"));
        final Coupon coupon = couponRepository.findIssuableById(couponId, LocalDateTime.now())
            .orElseThrow(() -> new CoreException(ErrorCode.COUPON_NOT_FOUND_OR_EXPIRED));
        final SignupCouponQuota quota = signupCouponQuotaRepository.findByCouponIdForUpdate(couponId).orElse(null);
        if (quota == null && IssueSignupCouponUseCase.SIGNUP_COUPON_ID.equals(couponId)) {
            throw new CoreException(ErrorCode.COUPON_NOT_FOUND_OR_EXPIRED);
        }
        if (quota != null && !quota.canIssue()) {
            throw new CoreException(ErrorCode.COUPON_NOT_FOUND_OR_EXPIRED);
        }
        final Optional<OwnedCoupon> existing = ownedCouponRepository.findByUserIdAndCouponId(userId, coupon.id());
        if (existing.isPresent()) {
            throw new CoreException(ErrorCode.COUPON_ALREADY_DOWNLOADED);
        }

        final OwnedCoupon ownedCoupon = new OwnedCoupon(userId, coupon.id(), OwnedCouponStatus.ISSUED, coupon.maxUseCount(), 0L);
        ownedCouponRepository.save(ownedCoupon);
        if (quota != null) {
            quota.issue();
        }
        log.info("쿠폰 발급 처리를 했습니다. couponId={}, traceId={}", couponId, MDC.get("traceId"));
    }
}
