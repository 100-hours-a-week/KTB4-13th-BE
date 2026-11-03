package com.book.core.coupon.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.book.core.coupon.application.port.CouponRepositoryPort;
import com.book.core.coupon.application.port.OwnedCouponRepositoryPort;
import com.book.core.coupon.domain.Coupon;
import com.book.core.coupon.domain.OwnedCoupon;
import com.book.core.coupon.domain.enums.CouponType;
import com.book.core.coupon.domain.enums.OwnedCouponStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class FindOwnedCouponsForCheckoutUseCaseTest {
    private final CouponRepositoryPort couponRepository = mock(CouponRepositoryPort.class);
    private final OwnedCouponRepositoryPort ownedCouponRepository = mock(OwnedCouponRepositoryPort.class);
    private final LocalDateTime now = LocalDateTime.of(2026, 10, 13, 9, 0);

    @Test
    void 체크아웃_조회는_대상_ID와_기준_시간을_전달한다() {
        when(ownedCouponRepository.findOwnedCouponIds(42L, Set.of(10L), now)).thenReturn(List.of(ownedCoupon(0L)));
        when(couponRepository.findAllById(Set.of(10L))).thenReturn(List.of(coupon(10L)));
        assertThat(new FindOwnedCouponsForCheckoutUseCase(ownedCouponRepository, couponRepository).execute(42L, Set.of(10L), now))
            .singleElement().satisfies((final var result) -> {
                assertThat(result.maxUseCount()).isEqualTo(2L);
                assertThat(result.usedCount()).isZero();
                assertThat(result.coupon().minOrderAmount()).isEqualByComparingTo("1000");
            });
    }

    @Test
    void 체크아웃_대상이나_보유_쿠폰이_없으면_빈_목록이다() {
        final var useCase = new FindOwnedCouponsForCheckoutUseCase(ownedCouponRepository, couponRepository);
        assertThat(useCase.execute(42L, Set.of(), now)).isEmpty();
        verifyNoInteractions(ownedCouponRepository, couponRepository);
        when(ownedCouponRepository.findOwnedCouponIds(42L, Set.of(10L), now)).thenReturn(List.of());
        assertThat(useCase.execute(42L, Set.of(10L), now)).isEmpty();
        verifyNoInteractions(couponRepository);
    }

    private Coupon coupon(final Long id) {
        return new Coupon(id, "쿠폰", CouponType.FIXED_AMOUNT, BigDecimal.TEN, 2L, new BigDecimal("1000"), now.plusDays(7), 100, 0,
            now.minusDays(1));
    }

    private OwnedCoupon ownedCoupon(final long usedCount) {
        return new OwnedCoupon(42L, 10L, OwnedCouponStatus.ISSUED, 2L, usedCount);
    }
}
