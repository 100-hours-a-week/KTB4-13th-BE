package com.book.core.coupon.application.usecase;

import com.book.common.annotation.UseCase;
import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import com.book.core.coupon.application.port.CouponRepositoryPort;
import com.book.core.coupon.application.port.OwnedCouponRepositoryPort;
import com.book.core.coupon.domain.Coupon;
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

    @Transactional
    public void execute(final Long userId, final Long couponId) {
        log.atDebug().addKeyValue("couponId", couponId).log("[COUPON] 쿠폰 발급 처리를 시작합니다. couponId={}", couponId);
        final Coupon coupon =
            couponRepository.findByIdWithLock(couponId).orElseThrow(() -> new CoreException(ErrorCode.COUPON_NOT_FOUND_OR_EXPIRED));

        if (!coupon.isIssuableAt(now())) {
            throw new CoreException(ErrorCode.COUPON_NOT_FOUND_OR_EXPIRED);
        }
        if (coupon.isSoldOut()) {
            throw new CoreException(ErrorCode.COUPON_SOLD_OUT);
        }
        if (ownedCouponRepository.existsByUserIdAndCouponId(userId, coupon.id())) {
            throw new CoreException(ErrorCode.COUPON_ALREADY_ISSUED);
        }
        coupon.issue();

        final OwnedCoupon ownedCoupon = OwnedCoupon.of(userId, coupon.id(), coupon.maxUseCount());
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
}
