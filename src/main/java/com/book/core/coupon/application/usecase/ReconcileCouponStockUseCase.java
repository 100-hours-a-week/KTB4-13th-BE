package com.book.core.coupon.application.usecase;

import com.book.common.annotation.UseCase;
import com.book.core.coupon.application.port.CouponRepositoryPort;
import com.book.core.coupon.application.port.CouponStockPort;
import com.book.core.coupon.application.result.CouponStockReconciliationResult;
import java.time.Clock;
import java.util.Optional;
import java.util.stream.Collectors;
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
            final boolean stable = before.equals(after);
            final boolean consistentQuantity = before.quantity() != null && before.quantity() >= 0
                && (long) counts.totalQuantity() == (long) counts.issuedQuantity() + before.quantity()
                && counts.issuedQuantity() == counts.ownedCouponCount();
            if (stable && consistentQuantity && pendingReservations == 0) {
                final var missing = counts.issuedUserIds().stream().filter(id -> !before.reservedUserIds().contains(id))
                    .collect(Collectors.toUnmodifiableSet());
                final boolean shouldBeSoldOut = before.quantity() == 0;
                if (!missing.isEmpty() || before.soldOutSignalPresent() != shouldBeSoldOut) {
                    final boolean repaired = couponStockPort.repairSnapshot(couponId, before, missing);
                    return new CouponStockReconciliationResult(couponId, clock.instant(), counts.totalQuantity(), counts.issuedQuantity(),
                        counts.ownedCouponCount(), before.quantity(), repaired ? shouldBeSoldOut : before.soldOutSignalPresent(), 0,
                        repaired);
                }
            }
            return new CouponStockReconciliationResult(couponId, clock.instant(), counts.totalQuantity(), counts.issuedQuantity(),
                counts.ownedCouponCount(), before.quantity(), before.soldOutSignalPresent(), pendingReservations, stable);
        });
    }
}
