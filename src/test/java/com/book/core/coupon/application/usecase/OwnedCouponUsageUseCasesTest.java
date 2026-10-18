package com.book.core.coupon.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import com.book.core.coupon.application.port.OwnedCouponRepositoryPort;
import com.book.core.coupon.domain.OwnedCoupon;
import com.book.core.coupon.domain.enums.OwnedCouponStatus;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;

class OwnedCouponUsageUseCasesTest {
    private final OwnedCouponRepositoryPort ownedCouponRepository = mock(OwnedCouponRepositoryPort.class);

    @Test
    void 사용과_복원은_변경_상태를_저장한다() {
        final var ownedCoupon = ownedCoupon(0L);
        when(ownedCouponRepository.findById(1L)).thenReturn(Optional.of(ownedCoupon));
        new UseOwnedCouponUseCase(ownedCouponRepository).execute(1L);
        assertThat(ownedCoupon.usedCount()).isEqualTo(1L);
        new RevertOwnedCouponUseCase(ownedCouponRepository).execute(1L);
        assertThat(ownedCoupon.usedCount()).isZero();
    }

    @Test
    void 사용과_복원의_없는_쿠폰은_OWNED_COUPON_INVALID다() {
        when(ownedCouponRepository.findById(1L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> new UseOwnedCouponUseCase(ownedCouponRepository).execute(1L)).isInstanceOf(CoreException.class)
            .extracting("errorCode").isEqualTo(ErrorCode.OWNED_COUPON_INVALID);
        assertThatThrownBy(() -> new RevertOwnedCouponUseCase(ownedCouponRepository).execute(1L)).isInstanceOf(CoreException.class)
            .extracting("errorCode").isEqualTo(ErrorCode.OWNED_COUPON_INVALID);
    }

    @Test
    void 초과_사용과_미사용_복원은_OWNED_COUPON_INVALID_USAGE다() {
        final var fullyUsed = new OwnedCoupon(42L, 10L, OwnedCouponStatus.USED, 2L, 2L);
        when(ownedCouponRepository.findById(1L)).thenReturn(Optional.of(fullyUsed));
        assertThatThrownBy(() -> new UseOwnedCouponUseCase(ownedCouponRepository).execute(1L)).isInstanceOf(CoreException.class)
            .extracting("errorCode").isEqualTo(ErrorCode.OWNED_COUPON_INVALID_USAGE);
        assertThat(fullyUsed.usedCount()).isEqualTo(2L);
        final var unused = ownedCoupon(0L);
        when(ownedCouponRepository.findById(1L)).thenReturn(Optional.of(unused));
        assertThatThrownBy(() -> new RevertOwnedCouponUseCase(ownedCouponRepository).execute(1L)).isInstanceOf(CoreException.class)
            .extracting("errorCode").isEqualTo(ErrorCode.OWNED_COUPON_INVALID_USAGE);
        assertThat(unused.usedCount()).isZero();
    }

    @Test
    void 한_번_사용_가능한_쿠폰의_마지막_사용과_복원_경계를_검증한다() {
        final var ownedCoupon = new OwnedCoupon(42L, 10L, OwnedCouponStatus.ISSUED, 1L);
        when(ownedCouponRepository.findById(1L)).thenReturn(Optional.of(ownedCoupon));
        final var use = new UseOwnedCouponUseCase(ownedCouponRepository);
        final var revert = new RevertOwnedCouponUseCase(ownedCouponRepository);

        use.execute(1L);
        assertThat(ownedCoupon.usedCount()).isEqualTo(1L);
        assertThat(ownedCoupon.status()).isEqualTo(OwnedCouponStatus.USED);
        assertThatThrownBy(() -> use.execute(1L)).isInstanceOf(CoreException.class).extracting("errorCode")
            .isEqualTo(ErrorCode.OWNED_COUPON_INVALID_USAGE);
        assertThat(ownedCoupon.usedCount()).isEqualTo(1L);

        revert.execute(1L);
        assertThat(ownedCoupon.usedCount()).isZero();
        assertThat(ownedCoupon.status()).isEqualTo(OwnedCouponStatus.ISSUED);
        assertThatThrownBy(() -> revert.execute(1L)).isInstanceOf(CoreException.class).extracting("errorCode")
            .isEqualTo(ErrorCode.OWNED_COUPON_INVALID_USAGE);
        assertThat(ownedCoupon.usedCount()).isZero();
    }

    @Test
    void 사용과_복원의_조회_실패는_그대로_전파한다() {
        final var failure = new DataAccessResourceFailureException("database failure");
        when(ownedCouponRepository.findById(1L)).thenThrow(failure);

        assertThatThrownBy(() -> new UseOwnedCouponUseCase(ownedCouponRepository).execute(1L)).isSameAs(failure);
        assertThatThrownBy(() -> new RevertOwnedCouponUseCase(ownedCouponRepository).execute(1L)).isSameAs(failure);
    }

    private OwnedCoupon ownedCoupon(final long usedCount) {
        return new OwnedCoupon(42L, 10L, OwnedCouponStatus.ISSUED, 2L, usedCount);
    }
}
