package com.book.core.coupon.application.usecase;

import com.book.common.annotation.UseCase;
import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import com.book.core.coupon.application.port.CouponRepositoryPort;
import com.book.core.coupon.application.port.OwnedCouponRepositoryPort;
import com.book.core.coupon.application.result.OwnedCouponResult;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
public class GetOwnedCouponUseCase {
    private final OwnedCouponRepositoryPort ownedCouponRepository;
    private final CouponRepositoryPort couponRepository;

    @Transactional(readOnly = true)
    public OwnedCouponResult execute(final Long ownedCouponId) {
        final var ownedCoupon =
            ownedCouponRepository.findById(ownedCouponId).orElseThrow(() -> new CoreException(ErrorCode.NOT_FOUND_DATA));
        final var coupon = couponRepository.findById(ownedCoupon.couponId()).orElseThrow(() -> new CoreException(ErrorCode.NOT_FOUND_DATA));
        return OwnedCouponResult.from(ownedCoupon, coupon);
    }
}
