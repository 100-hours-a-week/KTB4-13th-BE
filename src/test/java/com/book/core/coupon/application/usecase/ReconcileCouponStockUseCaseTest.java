package com.book.core.coupon.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.book.core.coupon.application.port.CouponIssuanceCounts;
import com.book.core.coupon.application.port.CouponRepositoryPort;
import com.book.core.coupon.application.port.CouponStockPort;
import com.book.core.coupon.application.port.CouponStockSnapshot;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class ReconcileCouponStockUseCaseTest {
    private static final long COUPON_ID = 10L;
    private static final Instant OBSERVED_AT = Instant.parse("2026-11-09T00:00:00Z");

    private final CouponRepositoryPort couponRepository = mock();
    private final CouponStockPort couponStockPort = mock();
    private final ReconcileCouponStockUseCase useCase = new ReconcileCouponStockUseCase(couponRepository, couponStockPort,
        Clock.fixed(OBSERVED_AT, ZoneOffset.UTC), new org.springframework.beans.factory.support.StaticListableBeanFactory()
            .getBeanProvider(com.book.core.coupon.application.port.CouponIssuePendingPort.class));

    @Test
    void 쿠폰이_없으면_Redis를_읽지_않는다() {
        when(couponRepository.findIssuanceCounts(COUPON_ID)).thenReturn(Optional.empty());

        assertThat(useCase.execute(COUPON_ID)).isEmpty();
        verifyNoInteractions(couponStockPort);
    }

    @Test
    void 발급_건수_불일치와_재고_초과를_별도로_보여준다() {
        when(couponRepository.findIssuanceCounts(COUPON_ID)).thenReturn(Optional.of(new CouponIssuanceCounts(3, 2, 1)));
        when(couponStockPort.readSnapshot(COUPON_ID)).thenReturn(new CouponStockSnapshot(2, false));

        final var result = useCase.execute(COUPON_ID).orElseThrow();

        assertThat(result.couponId()).isEqualTo(COUPON_ID);
        assertThat(result.observedAt()).isEqualTo(OBSERVED_AT);
        assertThat(result.stockDifference()).isEqualTo(-1L);
        assertThat(result.hasIssuanceCountMismatch()).isTrue();
        assertThat(result.hasUnexpectedSoldOutSignal()).isFalse();
        assertThat(result.isConsistentlySoldOut()).isFalse();
    }
}
