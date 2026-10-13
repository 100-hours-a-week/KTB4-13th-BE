package com.book.core.coupon.application.usecase;

import com.book.common.annotation.UseCase;
import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import com.book.core.coupon.application.port.OwnedCouponRepositoryPort;
import com.book.core.coupon.domain.OwnedCoupon;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@UseCase
@RequiredArgsConstructor
public class RevertOwnedCouponUseCase {
    private final OwnedCouponRepositoryPort ownedCouponRepository;

    @Transactional
    public void execute(final Long ownedCouponId) {
        final OwnedCoupon ownedCoupon =
            ownedCouponRepository.findById(ownedCouponId).orElseThrow(() -> new CoreException(ErrorCode.OWNED_COUPON_INVALID));

        if (ownedCoupon.isUnused()) {
            log.error("[OWNED_COUPON] 비정상적인 쿠폰 복원- 미사용 쿠폰 복원 시도 ownedCouponId={}", ownedCouponId);
            throw new CoreException(ErrorCode.OWNED_COUPON_INVALID_USAGE);
        }

        ownedCoupon.revert();
    }
}
