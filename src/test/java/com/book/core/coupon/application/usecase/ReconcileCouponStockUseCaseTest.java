package com.book.core.coupon.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.book.core.coupon.application.port.CouponIssuanceCounts;
import com.book.core.coupon.application.port.CouponRepositoryPort;
import com.book.core.coupon.application.port.CouponStockPort;
import com.book.core.coupon.application.port.CouponStockSnapshot;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.Set;
import com.book.core.coupon.application.result.CouponReconcileOutcome;
import org.junit.jupiter.api.Test;

class ReconcileCouponStockUseCaseTest {
    private static final long COUPON_ID = 10L;
    private static final Instant OBSERVED_AT = Instant.parse("2026-11-09T00:00:00Z");

    private final CouponRepositoryPort couponRepository = mock();
    private final CouponStockPort couponStockPort = mock();
    private final ReconcileCouponStockUseCase useCase =
        new ReconcileCouponStockUseCase(couponRepository, couponStockPort, Clock.fixed(OBSERVED_AT, ZoneOffset.UTC));

    @Test
    void 쿠폰이_없으면_대사_결과를_반환하지_않는다() {
        when(couponRepository.findIssuanceCounts(COUPON_ID)).thenReturn(Optional.empty());

        assertThat(useCase.execute(COUPON_ID)).isEmpty();
        verify(couponStockPort).readSnapshot(COUPON_ID);
    }

    @Test
    void 발급_건수_불일치와_재고_초과를_별도로_보여준다() {
        when(couponRepository.findIssuanceCounts(COUPON_ID)).thenReturn(Optional.of(new CouponIssuanceCounts(3, 2, Set.of(42L))));
        when(couponStockPort.readSnapshot(COUPON_ID)).thenReturn(new CouponStockSnapshot(2, false, Set.of(42L)));

        final var result = useCase.execute(COUPON_ID).orElseThrow();

        assertThat(result.couponId()).isEqualTo(COUPON_ID);
        assertThat(result.observedAt()).isEqualTo(OBSERVED_AT);
        assertThat(result.stockDifference()).isEqualTo(-1L);
        assertThat(result.outcome()).isEqualTo(CouponReconcileOutcome.DRIFT);
        assertThat(result.hasIssuanceCountMismatch()).isTrue();
        assertThat(result.hasUnexpectedSoldOutSignal()).isFalse();
        assertThat(result.isConsistentlySoldOut()).isFalse();
    }

    @Test
    void 예약과_DB_발급을_사용자별로_대조해_미반영_예약만_뺀다() {
        when(couponRepository.findIssuanceCounts(COUPON_ID)).thenReturn(Optional.of(new CouponIssuanceCounts(3, 1, Set.of(41L))));
        when(couponStockPort.readSnapshot(COUPON_ID)).thenReturn(new CouponStockSnapshot(1, false, Set.of(42L)));

        final var result = useCase.execute(COUPON_ID).orElseThrow();

        assertThat(result.pendingReservations()).isOne();
        assertThat(result.expectedStock()).isOne();
        assertThat(result.stockDifference()).isZero();
        assertThat(result.outcome()).isEqualTo(CouponReconcileOutcome.PENDING);
    }

    @Test
    void 대사_중_Redis_상태가_바뀌면_수량_차이를_판정하지_않는다() {
        when(couponRepository.findIssuanceCounts(COUPON_ID)).thenReturn(Optional.of(new CouponIssuanceCounts(3, 1, Set.of(41L))));
        when(couponStockPort.readSnapshot(COUPON_ID)).thenReturn(new CouponStockSnapshot(2, false, Set.of()),
            new CouponStockSnapshot(1, false, Set.of(42L)));

        final var result = useCase.execute(COUPON_ID).orElseThrow();

        assertThat(result.stockDifference()).isNull();
        assertThat(result.outcome()).isEqualTo(CouponReconcileOutcome.SNAPSHOT_CHANGED);
    }
}
