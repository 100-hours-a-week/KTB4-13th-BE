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
        final var before = couponStockPort.readSnapshot(couponId);
        return couponRepository.findIssuanceCounts(couponId).map(counts -> {
            final var after = couponStockPort.readSnapshot(couponId);
            final long pendingReservations =
                before.reservedUserIds().stream().filter(userId -> !counts.issuedUserIds().contains(userId)).count();
            return new CouponStockReconciliationResult(couponId, clock.instant(), counts.totalQuantity(), counts.issuedQuantity(),
                counts.ownedCouponCount(), before.quantity(), before.soldOutSignalPresent(), pendingReservations, before.equals(after));
        });
    }
}
