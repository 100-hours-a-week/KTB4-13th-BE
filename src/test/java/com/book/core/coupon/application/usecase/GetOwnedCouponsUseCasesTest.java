package com.book.core.coupon.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import com.book.core.coupon.application.port.CouponRepositoryPort;
import com.book.core.coupon.application.port.OwnedCouponRepositoryPort;
import com.book.core.coupon.domain.Coupon;
import com.book.core.coupon.domain.OwnedCoupon;
import com.book.core.coupon.domain.enums.CouponType;
import com.book.core.coupon.domain.enums.OwnedCouponStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

class GetOwnedCouponsUseCasesTest {
    private final CouponRepositoryPort couponRepository = mock(CouponRepositoryPort.class);
    private final OwnedCouponRepositoryPort ownedCouponRepository = mock(OwnedCouponRepositoryPort.class);
    private final LocalDateTime now = LocalDateTime.of(2026, 10, 13, 9, 0);

    @Test
    void 보유_목록은_사용_완료와_만료_쿠폰도_포함한다() {
        final var expiredCoupon =
            new Coupon(10L, "만료", CouponType.FIXED_AMOUNT, BigDecimal.TEN, 2L, BigDecimal.ZERO, now.minusDays(1), 100, 0, now.minusDays(2));
        when(ownedCouponRepository.findActiveByUserId(42L)).thenReturn(List.of(new OwnedCoupon(42L, 10L, OwnedCouponStatus.USED, 2L, 2L)));
        when(couponRepository.findAllById(Set.of(10L))).thenReturn(List.of(expiredCoupon));
        assertThat(new GetOwnedCouponsUseCase(ownedCouponRepository, couponRepository).execute(42L)).singleElement()
            .satisfies((final var result) -> {
                assertThat(result.userId()).isEqualTo(42L);
                assertThat(result.state()).isEqualTo(OwnedCouponStatus.USED);
                assertThat(result.coupon().expiredAt()).isEqualTo(now.minusDays(1));
            });
    }

    @Test
    void 보유_쿠폰이_없으면_쿠폰_정보를_조회하지_않는다() {
        when(ownedCouponRepository.findActiveByUserId(42L)).thenReturn(List.of());
        assertThat(new GetOwnedCouponsUseCase(ownedCouponRepository, couponRepository).execute(42L)).isEmpty();
        verifyNoInteractions(couponRepository);
    }

    @Test
    void 보유_목록의_원본_쿠폰이_일부_누락되면_NOT_FOUND_DATA다() {
        when(ownedCouponRepository.findActiveByUserId(42L))
            .thenReturn(List.of(ownedCoupon(0L), new OwnedCoupon(42L, 20L, OwnedCouponStatus.ISSUED, 2L)));
        when(couponRepository.findAllById(Set.of(10L, 20L))).thenReturn(List.of(coupon(10L)));

        assertThatThrownBy(() -> new GetOwnedCouponsUseCase(ownedCouponRepository, couponRepository).execute(42L))
            .isInstanceOf(CoreException.class).extracting("errorCode").isEqualTo(ErrorCode.NOT_FOUND_DATA);
    }

    @Test
    void 보유_단건은_쿠폰_정보를_함께_반환한다() {
        when(ownedCouponRepository.findById(1L)).thenReturn(Optional.of(ownedCoupon(0L)));
        when(couponRepository.findById(10L)).thenReturn(Optional.of(coupon(10L)));
        assertThat(new GetOwnedCouponUseCase(ownedCouponRepository, couponRepository).execute(1L).coupon().id()).isEqualTo(10L);
    }

    @Test
    void 보유_쿠폰_또는_원본_쿠폰이_없으면_NOT_FOUND_DATA다() {
        final var useCase = new GetOwnedCouponUseCase(ownedCouponRepository, couponRepository);
        when(ownedCouponRepository.findById(1L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> useCase.execute(1L)).isInstanceOf(CoreException.class).extracting("errorCode")
            .isEqualTo(ErrorCode.NOT_FOUND_DATA);
        when(ownedCouponRepository.findById(1L)).thenReturn(Optional.of(ownedCoupon(0L)));
        when(couponRepository.findById(10L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> useCase.execute(1L)).isInstanceOf(CoreException.class).extracting("errorCode")
            .isEqualTo(ErrorCode.NOT_FOUND_DATA);
    }

    private Coupon coupon(final Long id) {
        return new Coupon(id, "쿠폰", CouponType.FIXED_AMOUNT, BigDecimal.TEN, 2L, new BigDecimal("1000"), now.plusDays(7), 100, 0,
            now.minusDays(1));
    }

    private OwnedCoupon ownedCoupon(final long usedCount) {
        return new OwnedCoupon(42L, 10L, OwnedCouponStatus.ISSUED, 2L, usedCount);
    }
}
