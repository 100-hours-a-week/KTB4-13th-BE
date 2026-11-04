package com.book.core.coupon.application.usecase;

import com.book.common.annotation.UseCase;
import com.book.core.coupon.application.command.CreateCouponCommand;
import com.book.core.coupon.application.port.CouponRepositoryPort;
import com.book.core.coupon.application.port.CouponStockPort;
import com.book.core.coupon.application.result.CouponResult;
import com.book.core.coupon.domain.Coupon;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
public class CreateCouponUseCase {
    private final CouponRepositoryPort couponRepository;
    private final CouponStockPort couponStockPort;

    @Transactional
    public CouponResult execute(final CreateCouponCommand command) {
        final Coupon coupon = new Coupon(command.name(), command.type(), command.discount(), command.maxUseCount(),
            command.minOrderAmount(), command.expiredAt(), command.totalQuantity(), command.startsAt());
        final Coupon savedCoupon = couponRepository.save(coupon);

        couponStockPort.initialize(savedCoupon.id(), savedCoupon.totalQuantity());

        return CouponResult.from(savedCoupon);
    }
}
