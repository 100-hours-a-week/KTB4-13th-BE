package com.book.core.coupon.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import com.book.core.coupon.application.port.CouponCacheRepositoryPort;
import com.book.core.coupon.application.port.CouponLoadSimulationPort;
import com.book.core.coupon.application.port.CouponRepositoryPort;
import com.book.core.coupon.application.service.CacheMetrics;
import com.book.core.coupon.domain.Coupon;
import com.book.core.coupon.domain.CouponIssuePolicy;
import com.book.core.coupon.domain.enums.CouponType;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class GetCouponIssuePolicyUseCaseTest {
    private final CouponRepositoryPort couponRepository = mock(CouponRepositoryPort.class);
    private final CacheMetrics cacheMetrics = mock(CacheMetrics.class);
    private final CouponLoadSimulationPort couponLoadSimulationPort = mock(CouponLoadSimulationPort.class);
    private final CouponCacheRepositoryPort couponIssuePolicyCache = mock(CouponCacheRepositoryPort.class);
    private final GetCouponIssuePolicyUseCase useCase =
        new GetCouponIssuePolicyUseCase(couponRepository, cacheMetrics, couponLoadSimulationPort, couponIssuePolicyCache);
    private final LocalDateTime now = LocalDateTime.now();

    @Test
    void 캐시에_정책이_있으면_쿠폰_조회와_인위_지연을_생략한다() {
        final var policy = CouponIssuePolicy.from(coupon(10L));
        when(couponIssuePolicyCache.find(10L)).thenReturn(Optional.of(policy));

        assertThat(useCase.execute(10L)).isSameAs(policy);

        verify(cacheMetrics).incrementCouponCacheHit();
        verify(couponRepository, never()).findById(10L);
        verifyNoInteractions(couponLoadSimulationPort);
        verify(couponIssuePolicyCache, never()).save(10L, policy);
    }

    @Test
    void 캐시가_비어_있으면_지연_후_DB에서_정책을_읽어_저장한다() {
        final var coupon = coupon(10L);
        when(couponRepository.findById(10L)).thenReturn(Optional.of(coupon));

        final var policy = useCase.execute(10L);

        assertThat(policy).isEqualTo(CouponIssuePolicy.from(coupon));
        final var lookupOrder = inOrder(couponLoadSimulationPort, couponRepository, couponIssuePolicyCache);
        lookupOrder.verify(couponLoadSimulationPort).simulateCouponReadLatency();
        lookupOrder.verify(couponRepository).findById(10L);
        lookupOrder.verify(couponIssuePolicyCache).save(10L, policy);
        verify(cacheMetrics).incrementCouponDbRead();
    }

    @Test
    void 존재하지_않거나_비활성인_쿠폰은_정책을_저장하지_않는다() {
        when(couponRepository.findById(10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(10L)).isInstanceOf(CoreException.class).extracting("errorCode")
            .isEqualTo(ErrorCode.COUPON_NOT_FOUND_OR_EXPIRED);
        verify(couponIssuePolicyCache).find(10L);
        verifyNoMoreInteractions(couponIssuePolicyCache);
    }

    private Coupon coupon(final Long id) {
        return new Coupon(id, "쿠폰", CouponType.FIXED_AMOUNT, BigDecimal.TEN, 2L, new BigDecimal("1000"), now.plusDays(7), 10, 0,
            now.minusDays(1));
    }
}
