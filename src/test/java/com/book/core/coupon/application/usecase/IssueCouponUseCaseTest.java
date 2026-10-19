package com.book.core.coupon.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
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
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class IssueCouponUseCaseTest {
    private final CouponRepositoryPort couponRepository = mock(CouponRepositoryPort.class);
    private final OwnedCouponRepositoryPort ownedCouponRepository = mock(OwnedCouponRepositoryPort.class);
    private final LocalDateTime now = LocalDateTime.of(2026, 10, 13, 9, 0);

    @Test
    void 활성_미만료_쿠폰을_사용횟수_0으로_발급한다() {
        when(couponRepository.findIssuableById(eq(10L), any(LocalDateTime.class))).thenReturn(Optional.of(coupon(10L)));
        when(ownedCouponRepository.findByUserIdAndCouponId(42L, 10L)).thenReturn(Optional.empty());
        new IssueCouponUseCase(couponRepository, ownedCouponRepository).execute(42L, 10L);
        final var captured = ArgumentCaptor.forClass(OwnedCoupon.class);
        verify(ownedCouponRepository).save(captured.capture());
        final var ownedCoupon = captured.getValue();
        assertThat(ownedCoupon.id()).isNull();
        assertThat(ownedCoupon.userId()).isEqualTo(42L);
        assertThat(ownedCoupon.couponId()).isEqualTo(10L);
        assertThat(ownedCoupon.maxUseCount()).isEqualTo(2L);
        assertThat(ownedCoupon.usedCount()).isZero();
        assertThat(ownedCoupon.status()).isEqualTo(OwnedCouponStatus.ISSUED);
    }

    @Test
    void 만료_또는_비활성_쿠폰은_발급하지_않는다() {
        when(couponRepository.findIssuableById(eq(10L), any(LocalDateTime.class))).thenReturn(Optional.empty());
        assertThatThrownBy(() -> new IssueCouponUseCase(couponRepository, ownedCouponRepository).execute(42L, 10L))
            .isInstanceOf(CoreException.class).extracting("errorCode").isEqualTo(ErrorCode.COUPON_NOT_FOUND_OR_EXPIRED);
        verifyNoInteractions(ownedCouponRepository);
    }

    @Test
    void 이미_발급한_쿠폰은_다시_발급하지_않는다() {
        when(couponRepository.findIssuableById(eq(10L), any(LocalDateTime.class))).thenReturn(Optional.of(coupon(10L)));
        when(ownedCouponRepository.findByUserIdAndCouponId(42L, 10L)).thenReturn(Optional.of(ownedCoupon(0L)));
        assertThatThrownBy(() -> new IssueCouponUseCase(couponRepository, ownedCouponRepository).execute(42L, 10L))
            .isInstanceOf(CoreException.class).extracting("errorCode").isEqualTo(ErrorCode.COUPON_ALREADY_DOWNLOADED);
        verify(ownedCouponRepository, never()).save(any());
    }

    private Coupon coupon(final Long id) {
        return new Coupon(id, "쿠폰", CouponType.FIXED_AMOUNT, BigDecimal.TEN, 2L, new BigDecimal("1000"), now.plusDays(7));
    }

    private OwnedCoupon ownedCoupon(final long usedCount) {
        return new OwnedCoupon(42L, 10L, OwnedCouponStatus.ISSUED, 2L, usedCount);
    }
}
