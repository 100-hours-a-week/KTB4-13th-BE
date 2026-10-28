package com.book.core.coupon.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
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
    private final LocalDateTime now = LocalDateTime.now();

    @Test
    void 활성_미만료_쿠폰을_사용횟수_0으로_발급한다() {
        final Coupon coupon = coupon(10L);
        when(couponRepository.findActiveByIdForUpdate(10L)).thenReturn(Optional.of(coupon));
        when(ownedCouponRepository.issueIfAbsent(any())).thenReturn(true);
        new IssueCouponUseCase(couponRepository, ownedCouponRepository).execute(42L, 10L);
        final var captured = ArgumentCaptor.forClass(OwnedCoupon.class);
        verify(ownedCouponRepository).issueIfAbsent(captured.capture());
        final var ownedCoupon = captured.getValue();
        assertThat(coupon.issuedQuantity()).isEqualTo(1);
        assertThat(ownedCoupon.id()).isNull();
        assertThat(ownedCoupon.userId()).isEqualTo(42L);
        assertThat(ownedCoupon.couponId()).isEqualTo(10L);
        assertThat(ownedCoupon.maxUseCount()).isEqualTo(2L);
        assertThat(ownedCoupon.usedCount()).isZero();
        assertThat(ownedCoupon.status()).isEqualTo(OwnedCouponStatus.ISSUED);
    }

    @Test
    void 만료_또는_비활성_쿠폰은_발급하지_않는다() {
        when(couponRepository.findActiveByIdForUpdate(10L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> new IssueCouponUseCase(couponRepository, ownedCouponRepository).execute(42L, 10L))
            .isInstanceOf(CoreException.class).extracting("errorCode").isEqualTo(ErrorCode.COUPON_NOT_FOUND_OR_EXPIRED);
        verifyNoInteractions(ownedCouponRepository);
    }

    @Test
    void 이미_발급한_쿠폰은_다시_발급하지_않는다() {
        when(couponRepository.findActiveByIdForUpdate(10L)).thenReturn(Optional.of(coupon(10L)));
        when(ownedCouponRepository.issueIfAbsent(any())).thenReturn(false);
        assertThatThrownBy(() -> new IssueCouponUseCase(couponRepository, ownedCouponRepository).execute(42L, 10L))
            .isInstanceOf(CoreException.class).extracting("errorCode").isEqualTo(ErrorCode.COUPON_ALREADY_DOWNLOADED);
    }

    @Test
    void 발급_수량이_소진되면_발급하지_않는다() {
        final var coupon =
            new Coupon(10L, "쿠폰", CouponType.FIXED_AMOUNT, BigDecimal.TEN, 2L, BigDecimal.ZERO, now.plusDays(1), 1, 1, now.minusDays(1));
        when(couponRepository.findActiveByIdForUpdate(10L)).thenReturn(Optional.of(coupon));
        when(ownedCouponRepository.issueIfAbsent(any())).thenReturn(true);
        assertThatThrownBy(() -> new IssueCouponUseCase(couponRepository, ownedCouponRepository).execute(42L, 10L))
            .isInstanceOf(CoreException.class).extracting("errorCode").isEqualTo(ErrorCode.COUPON_ISSUE_LIMIT_EXCEEDED);
    }

    @Test
    void 발급_시작_전이거나_만료된_쿠폰은_발급하지_않는다() {
        final var future =
            new Coupon(10L, "쿠폰", CouponType.FIXED_AMOUNT, BigDecimal.TEN, 2L, BigDecimal.ZERO, now.plusDays(2), 1, 0, now.plusDays(1));
        final var expired =
            new Coupon(10L, "쿠폰", CouponType.FIXED_AMOUNT, BigDecimal.TEN, 2L, BigDecimal.ZERO, now.minusDays(1), 1, 0, now.minusDays(2));
        when(couponRepository.findActiveByIdForUpdate(10L)).thenReturn(Optional.of(future), Optional.of(expired));
        final var useCase = new IssueCouponUseCase(couponRepository, ownedCouponRepository);
        assertThatThrownBy(() -> useCase.execute(42L, 10L)).isInstanceOf(CoreException.class).extracting("errorCode")
            .isEqualTo(ErrorCode.COUPON_NOT_FOUND_OR_EXPIRED);
        assertThatThrownBy(() -> useCase.execute(42L, 10L)).isInstanceOf(CoreException.class).extracting("errorCode")
            .isEqualTo(ErrorCode.COUPON_NOT_FOUND_OR_EXPIRED);
        verifyNoInteractions(ownedCouponRepository);
    }

    @Test
    void 발급_기간은_시작_시각을_포함하고_만료_시각을_제외한다() {
        final var coupon = new Coupon(10L, "쿠폰", CouponType.FIXED_AMOUNT, BigDecimal.TEN, 2L, BigDecimal.ZERO, now.plusDays(1), 1, 0, now);
        assertThat(coupon.isIssuableAt(now.minusNanos(1))).isFalse();
        assertThat(coupon.isIssuableAt(now)).isTrue();
        assertThat(coupon.isIssuableAt(now.plusDays(1))).isFalse();
    }

    private Coupon coupon(final Long id) {
        return new Coupon(id, "쿠폰", CouponType.FIXED_AMOUNT, BigDecimal.TEN, 2L, new BigDecimal("1000"), now.plusDays(7), 10, 0,
            now.minusDays(1));
    }

}
