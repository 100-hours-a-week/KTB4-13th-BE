package com.book.core.coupon.application.usecase;

import com.book.common.annotation.UseCase;
import com.book.core.coupon.application.port.CouponRepositoryPort;
import com.book.core.coupon.application.port.CouponStockPort;
import com.book.core.coupon.application.result.CouponStockReconciliationResult;
import java.time.Clock;
import java.util.Optional;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class ReconcileCouponStockUseCase {
    private final CouponRepositoryPort couponRepository;
    private final CouponStockPort couponStockPort;
    private final Clock clock;

    public Optional<CouponStockReconciliationResult> execute(final long couponId) {
        return couponRepository.findIssuanceCounts(couponId).map((final var counts) -> {
            final var stock = couponStockPort.readSnapshot(couponId);
            return new CouponStockReconciliationResult(couponId, clock.instant(), counts.totalQuantity(), counts.issuedQuantity(),
                counts.ownedCouponCount(), stock.quantity(), stock.soldOutSignalPresent());
        });
    }
}
