package com.book.core.coupon.application.usecase;

import com.book.common.annotation.UseCase;
import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import com.book.core.coupon.application.port.CouponRepositoryPort;
import com.book.core.coupon.application.port.OwnedCouponRepositoryPort;
import com.book.core.coupon.domain.Coupon;
import com.book.core.coupon.domain.OwnedCoupon;
import com.book.core.coupon.domain.enums.OwnedCouponStatus;

import java.time.LocalDateTime;
import java.util.Optional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
@Slf4j
public class IssueCouponUseCase {
    private final CouponRepositoryPort couponRepository;
    private final OwnedCouponRepositoryPort ownedCouponRepository;

    @Transactional
    public void execute(final Long userId, final Long couponId) {
        log.atDebug().addKeyValue("couponId", couponId).log("[COUPON] 쿠폰 발급 처리를 시작합니다. couponId={}", couponId);
        final Coupon coupon = couponRepository.findIssuableById(couponId, LocalDateTime.now())
            .orElseThrow(() -> new CoreException(ErrorCode.COUPON_NOT_FOUND_OR_EXPIRED));
        final Optional<OwnedCoupon> existing = ownedCouponRepository.findByUserIdAndCouponId(userId, coupon.id());
        if (existing.isPresent()) {
            throw new CoreException(ErrorCode.COUPON_ALREADY_DOWNLOADED);
        }

        final OwnedCoupon ownedCoupon = new OwnedCoupon(userId, coupon.id(), OwnedCouponStatus.ISSUED, coupon.maxUseCount(), 0L);
        ownedCouponRepository.save(ownedCoupon);
        log.atInfo().addKeyValue("couponId", couponId).log("[COUPON] 쿠폰 발급 처리를 마쳤습니다. couponId={}", couponId);
    }
}
