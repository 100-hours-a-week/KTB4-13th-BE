package com.book.core.coupon.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.book.core.coupon.application.command.CreateCouponCommand;
import com.book.core.coupon.application.port.CouponRepositoryPort;
import com.book.core.coupon.application.port.CouponStockPort;
import com.book.core.coupon.domain.Coupon;
import com.book.core.coupon.domain.enums.CouponType;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class CreateCouponUseCaseTest {
    private final CouponRepositoryPort couponRepository = mock(CouponRepositoryPort.class);
    private final CouponStockPort couponStockPort = mock(CouponStockPort.class);
    private final CreateCouponUseCase useCase = new CreateCouponUseCase(couponRepository, couponStockPort);

    @Test
    void 저장한_쿠폰의_발급_수량을_재고로_초기화한다() {
        final LocalDateTime startsAt = LocalDateTime.now();
        final LocalDateTime expiredAt = startsAt.plusDays(7);
        final var savedCoupon =
            new Coupon(10L, "신규 쿠폰", CouponType.FIXED_AMOUNT, BigDecimal.TEN, 2L, BigDecimal.ZERO, expiredAt, 5, 0, startsAt);
        when(couponRepository.save(any(Coupon.class))).thenReturn(savedCoupon);
        final var command =
            new CreateCouponCommand("신규 쿠폰", CouponType.FIXED_AMOUNT, BigDecimal.TEN, 2L, BigDecimal.ZERO, 5, startsAt, expiredAt);

        final var result = useCase.execute(command);

        assertThat(result.id()).isEqualTo(10L);
        verify(couponStockPort).initialize(10L, 5);
    }
}
