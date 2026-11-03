package com.book.core.coupon.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.book.core.coupon.application.port.CouponRepositoryPort;
import com.book.core.coupon.domain.Coupon;
import com.book.core.coupon.domain.enums.CouponType;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class GetCouponsUseCaseTest {
    private final CouponRepositoryPort couponRepository = mock(CouponRepositoryPort.class);
    private final LocalDateTime now = LocalDateTime.of(2026, 10, 13, 9, 0);

    @Test
    void 상품용_쿠폰은_활성_여부만_필터한다() {
        final var expiredCoupon = new Coupon(10L, "만료", CouponType.PERCENT_RATE, new BigDecimal("0.10"), 2L, new BigDecimal("1000"),
            now.minusDays(1), 100, 0, now.minusDays(2));
        when(couponRepository.findActiveByIds(Set.of(10L))).thenReturn(List.of(expiredCoupon));
        final var useCase = new GetCouponsUseCase(couponRepository);
        assertThat(useCase.execute(List.of())).isEmpty();
        assertThat(useCase.execute(Set.of(10L))).singleElement().satisfies((final var result) -> {
            assertThat(result.expiredAt()).isEqualTo(now.minusDays(1));
            assertThat(result.type()).isEqualTo(CouponType.PERCENT_RATE);
            assertThat(result.discount()).isEqualByComparingTo("0.10");
        });
    }
}
