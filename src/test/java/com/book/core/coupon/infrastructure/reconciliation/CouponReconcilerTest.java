package com.book.core.coupon.infrastructure.reconciliation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import com.book.core.coupon.application.usecase.ReconcileCouponStockUseCase;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class CouponReconcilerTest {
    private final ReconcileCouponStockUseCase reconcile = mock();
    private final ApplicationContextRunner context = new ApplicationContextRunner().withUserConfiguration(CouponReconciler.class)
        .withBean(ReconcileCouponStockUseCase.class, () -> reconcile);

    @Test
    void 지정된_쿠폰이_없으면_정기_대사를_등록하지_않는다() {
        context.run(application -> assertThat(application).doesNotHaveBean(CouponReconciler.class));
    }

    @Test
    void 지정한_쿠폰만_대사한다() {
        context.withPropertyValues("coupon.reconcile.coupon-ids=10,20").run(application -> {
            application.getBean(CouponReconciler.class).reconcileConfiguredCoupons();

            verify(reconcile).execute(10L);
            verify(reconcile).execute(20L);
            verifyNoMoreInteractions(reconcile);
        });
    }
}
