package com.book.core.coupon.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.book.core.coupon.application.command.IssuanceRequested;
import com.book.core.coupon.application.port.CouponRepositoryPort;
import com.book.core.coupon.application.port.OwnedCouponRepositoryPort;
import com.book.core.coupon.domain.OwnedCoupon;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class PersistAcceptedCouponIssueUseCaseTest {
    private final CouponRepositoryPort coupons = mock(CouponRepositoryPort.class);
    private final OwnedCouponRepositoryPort ownedCoupons = mock(OwnedCouponRepositoryPort.class);
    private final PersistAcceptedCouponIssueUseCase useCase = new PersistAcceptedCouponIssueUseCase(coupons, ownedCoupons);
    private final IssuanceRequested issue = new IssuanceRequested(10L, 42L, 2L);

    @Test
    void 접수된_발급의_수량과_보유_쿠폰을_저장한다() {
        when(coupons.incrementIssuedQuantity(10L)).thenReturn(1);

        assertThat(useCase.execute(issue)).isTrue();

        final var saved = ArgumentCaptor.forClass(OwnedCoupon.class);
        verify(ownedCoupons).save(saved.capture());
        assertThat(saved.getValue()).extracting("couponId", "userId", "maxUseCount").containsExactly(10L, 42L, 2L);
    }

    @Test
    void 이미_저장한_발급은_재처리해도_수량을_늘리지_않는다() {
        when(ownedCoupons.existsByUserIdAndCouponId(42L, 10L)).thenReturn(true);

        assertThat(useCase.execute(issue)).isFalse();

        verifyNoInteractions(coupons);
        verify(ownedCoupons, never()).save(any());
    }

    @Test
    void 발급_수량을_반영할_수_없으면_보유_쿠폰을_저장하지_않는다() {
        assertThatThrownBy(() -> useCase.execute(issue)).isInstanceOf(IllegalStateException.class);

        verify(ownedCoupons, never()).save(any());
    }
}
