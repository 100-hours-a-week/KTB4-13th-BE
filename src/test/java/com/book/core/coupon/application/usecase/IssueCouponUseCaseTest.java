package com.book.core.coupon.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import com.book.core.coupon.application.port.CouponRepositoryPort;
import com.book.core.coupon.application.port.CouponStockPort;
import com.book.core.coupon.application.port.IssueResult;
import com.book.core.coupon.application.port.OwnedCouponRepositoryPort;
import com.book.core.coupon.domain.Coupon;
import com.book.core.coupon.domain.CouponIssuePolicy;
import com.book.core.coupon.domain.OwnedCoupon;
import com.book.core.coupon.domain.enums.CouponType;
import com.book.core.coupon.domain.enums.OwnedCouponStatus;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Optional;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DataIntegrityViolationException;

class IssueCouponUseCaseTest {
    private final CouponRepositoryPort couponRepository = mock(CouponRepositoryPort.class);
    private final CouponStockPort couponStockPort = mock(CouponStockPort.class);
    private final OwnedCouponRepositoryPort ownedCouponRepository = mock(OwnedCouponRepositoryPort.class);
    private final LocalDateTime now = LocalDateTime.now();
    private final CouponIssuePolicy activePolicy = CouponIssuePolicy.from(coupon(10L));

    @BeforeEach
    void setCouponStockAvailable() {
        when(couponStockPort.tryIssue(anyLong())).thenReturn(IssueResult.ISSUED);
        when(couponRepository.incrementIssuedQuantity(anyLong())).thenReturn(1);
    }

    @Test
    void 활성_미만료_쿠폰을_사용횟수_0으로_발급한다() {
        when(ownedCouponRepository.existsByUserIdAndCouponId(42L, 10L)).thenReturn(false);
        when(couponStockPort.tryIssue(10L)).thenReturn(IssueResult.ISSUED);
        issueCouponUseCase().execute(42L, 10L, activePolicy);
        final var captured = ArgumentCaptor.forClass(OwnedCoupon.class);
        verify(ownedCouponRepository).save(captured.capture());
        verify(couponRepository).incrementIssuedQuantity(10L);
        final var ownedCoupon = captured.getValue();
        assertThat(ownedCoupon.id()).isNull();
        assertThat(ownedCoupon.userId()).isEqualTo(42L);
        assertThat(ownedCoupon.couponId()).isEqualTo(10L);
        assertThat(ownedCoupon.maxUseCount()).isEqualTo(2L);
        assertThat(ownedCoupon.usedCount()).isZero();
        assertThat(ownedCoupon.status()).isEqualTo(OwnedCouponStatus.ISSUED);
    }

    @Test
    void 캐시의_매진_상태가_오래되어도_DB_발급_한도를_넘기지_않는다() {
        final var coupon = coupon(10L);
        when(couponRepository.incrementIssuedQuantity(10L)).thenReturn(0);
        when(couponRepository.findById(10L)).thenReturn(Optional.of(coupon));

        assertThatThrownBy(() -> issueCouponUseCase().execute(42L, 10L, activePolicy)).isInstanceOf(CoreException.class)
            .extracting("errorCode").isEqualTo(ErrorCode.COUPON_SOLD_OUT);
        verify(ownedCouponRepository, never()).save(any(OwnedCoupon.class));
    }

    @Test
    void 이미_발급한_쿠폰은_다시_발급하지_않는다() {
        when(ownedCouponRepository.existsByUserIdAndCouponId(42L, 10L)).thenReturn(true);
        assertThatThrownBy(() -> issueCouponUseCase().execute(42L, 10L, activePolicy)).isInstanceOf(CoreException.class)
            .extracting("errorCode").isEqualTo(ErrorCode.COUPON_ALREADY_ISSUED);
    }

    @Test
    void 저장_중_보유_쿠폰_유일_제약에_걸리면_중복_발급_오류로_변환한다() {
        final var exception = new ConstraintViolationException("duplicate", new SQLException(), "owned_coupon.udx_owned_coupon");
        when(ownedCouponRepository.save(any(OwnedCoupon.class))).thenThrow(exception);

        assertThatThrownBy(() -> issueCouponUseCase().execute(42L, 10L, activePolicy)).isInstanceOfSatisfying(CoreException.class,
            (final CoreException actual) -> {
                assertThat(actual.errorCode()).isEqualTo(ErrorCode.COUPON_ALREADY_ISSUED);
                assertThat(actual.getCause()).isSameAs(exception);
            });
    }

    @Test
    void 저장_중_다른_무결성_오류는_그대로_전파한다() {
        final var cause = new ConstraintViolationException("invalid", new SQLException(), "another_constraint");
        final var exception = new DataIntegrityViolationException("invalid", cause);
        when(ownedCouponRepository.save(any(OwnedCoupon.class))).thenThrow(exception);

        assertThatThrownBy(() -> issueCouponUseCase().execute(42L, 10L, activePolicy)).isSameAs(exception);
    }

    @Test
    void 발급_수량이_소진되면_발급하지_않는다() {
        final var coupon =
            new Coupon(10L, "쿠폰", CouponType.FIXED_AMOUNT, BigDecimal.TEN, 2L, BigDecimal.ZERO, now.plusDays(1), 1, 1, now.minusDays(1));
        assertThatThrownBy(() -> issueCouponUseCase().execute(42L, 10L, CouponIssuePolicy.from(coupon))).isInstanceOf(CoreException.class)
            .extracting("errorCode").isEqualTo(ErrorCode.COUPON_SOLD_OUT);
        verifyNoInteractions(ownedCouponRepository);
    }

    @Test
    void Redis_재고가_소진되면_DB_발급량과_보유_쿠폰을_변경하지_않는다() {
        when(couponStockPort.tryIssue(10L)).thenReturn(IssueResult.SOLD_OUT);

        assertThatThrownBy(() -> issueCouponUseCase().execute(42L, 10L, activePolicy)).isInstanceOf(CoreException.class)
            .extracting("errorCode").isEqualTo(ErrorCode.COUPON_SOLD_OUT);

        verify(ownedCouponRepository, never()).save(any(OwnedCoupon.class));
        verify(couponRepository, org.mockito.Mockito.never()).incrementIssuedQuantity(10L);
    }

    @Test
    void Redis_발급_결과를_알_수_없으면_발급_실패로_처리한다() {
        when(couponStockPort.tryIssue(10L)).thenReturn(IssueResult.FAILED);

        assertThatThrownBy(() -> issueCouponUseCase().execute(42L, 10L, activePolicy)).isInstanceOf(CoreException.class)
            .extracting("errorCode").isEqualTo(ErrorCode.COUPON_ISSUE_FAILED);

        verify(ownedCouponRepository, never()).save(any(OwnedCoupon.class));
        verify(couponRepository, never()).incrementIssuedQuantity(10L);
    }

    @Test
    void 발급_시작_전이거나_만료된_쿠폰은_발급하지_않는다() {
        final var future =
            new Coupon(10L, "쿠폰", CouponType.FIXED_AMOUNT, BigDecimal.TEN, 2L, BigDecimal.ZERO, now.plusDays(2), 1, 0, now.plusDays(1));
        final var expired =
            new Coupon(10L, "쿠폰", CouponType.FIXED_AMOUNT, BigDecimal.TEN, 2L, BigDecimal.ZERO, now.minusDays(1), 1, 0, now.minusDays(2));
        final var useCase = issueCouponUseCase();
        assertThatThrownBy(() -> useCase.execute(42L, 10L, CouponIssuePolicy.from(future))).isInstanceOf(CoreException.class)
            .extracting("errorCode").isEqualTo(ErrorCode.COUPON_NOT_FOUND_OR_EXPIRED);
        assertThatThrownBy(() -> useCase.execute(42L, 10L, CouponIssuePolicy.from(expired))).isInstanceOf(CoreException.class)
            .extracting("errorCode").isEqualTo(ErrorCode.COUPON_NOT_FOUND_OR_EXPIRED);
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

    private IssueCouponUseCase issueCouponUseCase() {
        return new IssueCouponUseCase(couponRepository, ownedCouponRepository, couponStockPort);
    }

}
