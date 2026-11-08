package com.book.core.coupon.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import com.book.core.coupon.application.port.CouponCacheRepositoryPort;
import com.book.core.coupon.application.result.CouponIssueState;
import com.book.core.coupon.application.result.CouponIssueState.Stock;
import com.book.core.coupon.application.port.CouponLoadSimulationPort;
import com.book.core.coupon.application.port.CouponRepositoryPort;
import com.book.core.coupon.application.service.CacheMetrics;
import com.book.core.coupon.domain.Coupon;
import com.book.core.coupon.domain.CouponIssuePolicy;
import com.book.core.coupon.domain.enums.CouponType;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.SimpleTransactionStatus;

class GetCouponIssuePolicyUseCaseTest {
    private final CouponRepositoryPort couponRepository = mock(CouponRepositoryPort.class);
    private final CacheMetrics cacheMetrics = mock(CacheMetrics.class);
    private final CouponLoadSimulationPort couponLoadSimulationPort = mock(CouponLoadSimulationPort.class);
    private final CouponCacheRepositoryPort couponIssuePolicyCache = mock(CouponCacheRepositoryPort.class);
    private final PlatformTransactionManager transactionManager = mock(PlatformTransactionManager.class);
    private final SimpleTransactionStatus transactionStatus = new SimpleTransactionStatus();
    private final GetCouponIssuePolicyUseCase useCase = new GetCouponIssuePolicyUseCase(couponRepository, cacheMetrics,
        couponLoadSimulationPort, couponIssuePolicyCache, transactionManager);
    private final LocalDateTime now = LocalDateTime.now();

    @Test
    void 캐시에_정책이_있으면_쿠폰_조회와_인위_지연을_생략한다() {
        final var policy = CouponIssuePolicy.from(coupon(10L));
        when(couponIssuePolicyCache.getOrLoad(eq(10L), any())).thenReturn(new CouponIssueState(policy, Stock.UNKNOWN));

        assertThat(useCase.execute(10L).policy()).isSameAs(policy);

        verifyNoInteractions(couponRepository, couponLoadSimulationPort, cacheMetrics, transactionManager);
    }

    @Test
    void 캐시가_로딩을_요청하면_지연_후_DB에서_정책을_읽는다() {
        runPolicyLoader();
        final var coupon = coupon(10L);
        when(couponRepository.findById(10L)).thenReturn(Optional.of(coupon));

        final var policy = useCase.execute(10L).policy();

        assertThat(policy).isEqualTo(CouponIssuePolicy.from(coupon));
        final var lookupOrder = inOrder(couponLoadSimulationPort, transactionManager, couponRepository);
        lookupOrder.verify(couponLoadSimulationPort).simulateCouponReadLatency();
        lookupOrder.verify(transactionManager).getTransaction(argThat(TransactionDefinition::isReadOnly));
        lookupOrder.verify(couponRepository).findById(10L);
        lookupOrder.verify(transactionManager).commit(transactionStatus);
        verify(cacheMetrics).incrementCouponDbRead();
    }

    @Test
    void 존재하지_않거나_비활성인_쿠폰은_기존_오류를_반환한다() {
        runPolicyLoader();
        when(couponRepository.findById(10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(10L)).isInstanceOf(CoreException.class).extracting("errorCode")
            .isEqualTo(ErrorCode.COUPON_NOT_FOUND_OR_EXPIRED);
        verify(cacheMetrics).incrementCouponDbRead();
    }

    private void runPolicyLoader() {
        when(transactionManager.getTransaction(any())).thenReturn(transactionStatus);
        when(couponIssuePolicyCache.getOrLoad(eq(10L), any())).thenAnswer(
            (final var invocation) -> new CouponIssueState(invocation.<Supplier<CouponIssuePolicy>>getArgument(1).get(), Stock.UNKNOWN));
    }

    private Coupon coupon(final Long id) {
        return new Coupon(id, "쿠폰", CouponType.FIXED_AMOUNT, BigDecimal.TEN, 2L, new BigDecimal("1000"), now.plusDays(7), 10, 0,
            now.minusDays(1));
    }
}
