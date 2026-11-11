package com.book.core.coupon.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import com.book.core.coupon.application.port.CouponRepositoryPort;
import com.book.core.coupon.application.port.CouponStockPort;
import com.book.core.coupon.application.port.OwnedCouponRepositoryPort;
import com.book.core.coupon.application.result.CouponIssueState;
import com.book.core.coupon.application.result.CouponIssueState.Stock;
import com.book.core.coupon.domain.Coupon;
import com.book.core.coupon.domain.CouponIssuePolicy;
import com.book.core.coupon.domain.enums.CouponType;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class IssueCouponUseCaseTest {
    private final CouponRepositoryPort couponRepository = mock(CouponRepositoryPort.class);
    private final CouponStockPort couponStockPort = mock(CouponStockPort.class);
    private final OwnedCouponRepositoryPort ownedCouponRepository = mock(OwnedCouponRepositoryPort.class);
    private final LocalDateTime now = LocalDateTime.now();
    private final CouponIssueState activeIssueState = withoutStock(CouponIssuePolicy.from(coupon(10L)));

    @Test
    void 이미_발급한_쿠폰은_다시_발급하지_않는다() {
        when(ownedCouponRepository.existsByUserIdAndCouponId(42L, 10L)).thenReturn(true);
        assertThatThrownBy(() -> issueCouponUseCase().validateIssuable(42L, 10L, activeIssueState)).isInstanceOf(CoreException.class)
            .extracting("errorCode").isEqualTo(ErrorCode.COUPON_ALREADY_ISSUED);
    }

    @Test
    void 발급_수량이_소진되면_발급하지_않는다() {
        final var coupon =
            new Coupon(10L, "쿠폰", CouponType.FIXED_AMOUNT, BigDecimal.TEN, 2L, BigDecimal.ZERO, now.plusDays(1), 1, 1, now.minusDays(1));
        assertThatThrownBy(() -> issueCouponUseCase().validateIssuable(42L, 10L, withoutStock(CouponIssuePolicy.from(coupon))))
            .isInstanceOf(CoreException.class).extracting("errorCode").isEqualTo(ErrorCode.COUPON_SOLD_OUT);
        verifyNoInteractions(ownedCouponRepository);
    }

    @Test
    void 발급_시작_전이거나_만료된_쿠폰은_발급하지_않는다() {
        final var future =
            new Coupon(10L, "쿠폰", CouponType.FIXED_AMOUNT, BigDecimal.TEN, 2L, BigDecimal.ZERO, now.plusDays(2), 1, 0, now.plusDays(1));
        final var expired =
            new Coupon(10L, "쿠폰", CouponType.FIXED_AMOUNT, BigDecimal.TEN, 2L, BigDecimal.ZERO, now.minusDays(1), 1, 0, now.minusDays(2));
        final var useCase = issueCouponUseCase();
        assertThatThrownBy(() -> useCase.validateIssuable(42L, 10L, withoutStock(CouponIssuePolicy.from(future))))
            .isInstanceOf(CoreException.class).extracting("errorCode").isEqualTo(ErrorCode.COUPON_NOT_FOUND_OR_EXPIRED);
        assertThatThrownBy(() -> useCase.validateIssuable(42L, 10L, withoutStock(CouponIssuePolicy.from(expired))))
            .isInstanceOf(CoreException.class).extracting("errorCode").isEqualTo(ErrorCode.COUPON_NOT_FOUND_OR_EXPIRED);
        verifyNoInteractions(ownedCouponRepository);
    }

    @Test
    void 발급_기간은_시작_시각을_포함하고_만료_시각을_제외한다() {
        final var coupon = new Coupon(10L, "쿠폰", CouponType.FIXED_AMOUNT, BigDecimal.TEN, 2L, BigDecimal.ZERO, now.plusDays(1), 1, 0, now);
        assertThat(coupon.isIssuableAt(now.minusNanos(1))).isFalse();
        assertThat(coupon.isIssuableAt(now)).isTrue();
        assertThat(coupon.isIssuableAt(now.plusDays(1))).isFalse();
    }

    private CouponIssueState withoutStock(final CouponIssuePolicy policy) {
        return new CouponIssueState(policy, Stock.UNKNOWN);
    }

    private Coupon coupon(final Long id) {
        return new Coupon(id, "쿠폰", CouponType.FIXED_AMOUNT, BigDecimal.TEN, 2L, new BigDecimal("1000"), now.plusDays(7), 10, 0,
            now.minusDays(1));
    }

    private IssueCouponUseCase issueCouponUseCase() {
        return new IssueCouponUseCase(couponRepository, ownedCouponRepository, couponStockPort);
    }

}
