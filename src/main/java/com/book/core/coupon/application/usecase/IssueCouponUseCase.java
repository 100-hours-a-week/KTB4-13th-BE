package com.book.core.coupon.application.usecase;

import com.book.common.annotation.UseCase;
import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import com.book.core.coupon.application.port.CouponRepositoryPort;
import com.book.core.coupon.application.port.CouponStockPort;
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
    private final CouponStockPort couponStockPort;

    @Transactional
    public void execute(final Long userId, final Long couponId) {
        log.atDebug().addKeyValue("couponId", couponId).log("[COUPON] 쿠폰 발급 처리를 시작합니다. couponId={}", couponId);
        final Coupon coupon =
            couponRepository.findById(couponId).orElseThrow(() -> new CoreException(ErrorCode.COUPON_NOT_FOUND_OR_EXPIRED));

        if (!coupon.isIssuableAt(now())) {
            throw new CoreException(ErrorCode.COUPON_NOT_FOUND_OR_EXPIRED);
        }
        if (coupon.isSoldOut()) {
            throw new CoreException(ErrorCode.COUPON_SOLD_OUT);
        }
        if (ownedCouponRepository.existsByUserIdAndCouponId(userId, coupon.id())) {
            throw new CoreException(ErrorCode.COUPON_ALREADY_ISSUED);
        }

        tryIssue(couponId);
        couponRepository.incrementIssuedQuantity(couponId);

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

    private void tryIssue(final Long couponId) {
        switch (couponStockPort.tryIssue(couponId)) {
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
