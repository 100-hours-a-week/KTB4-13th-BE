package com.book.core.coupon.infrastructure.reconciliation;

import com.book.core.coupon.application.usecase.ReconcileCouponStockUseCase;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "coupon.reconcile.coupon-ids")
@RequiredArgsConstructor
@Slf4j
class CouponReconciler {
    private final ReconcileCouponStockUseCase reconcile;

    private List<Long> couponIds;

    @Value("${coupon.reconcile.coupon-ids}")
    void configureCouponIds(final String ids) {
        couponIds = Arrays.stream(ids.split(",")).map(String::strip).map(Long::valueOf).distinct().toList();
    }

    @Bean(defaultCandidate = false, destroyMethod = "shutdown")
    ScheduledExecutorService couponReconcileExecutor() {
        return Executors.newSingleThreadScheduledExecutor();
    }

    @Scheduled(fixedDelayString = "${coupon.reconcile.delay-ms:60000}", scheduler = "couponReconcileExecutor")
    void reconcileConfiguredCoupons() {
        for (final long couponId : couponIds) {
            try {
                reconcile.execute(couponId).ifPresentOrElse(
                    result -> log.atInfo().addKeyValue("couponId", couponId).addKeyValue("outcome", result.outcome())
                        .addKeyValue("totalQuantity", result.totalQuantity()).addKeyValue("issuedQuantity", result.issuedQuantity())
                        .addKeyValue("ownedCouponCount", result.ownedCouponCount()).addKeyValue("redisStock", result.redisStock())
                        .addKeyValue("pendingReservations", result.pendingReservations())
                        .addKeyValue("stockDifference", result.stockDifference()).log("[COUPON] 재고 대사"),
                    () -> log.atWarn().addKeyValue("couponId", couponId).log("[COUPON] 대사 대상 쿠폰을 찾을 수 없습니다."));
            } catch (final RuntimeException failure) {
                log.atWarn().addKeyValue("couponId", couponId).addKeyValue("errorType", failure.getClass().getSimpleName())
                    .log("[COUPON] 재고 대사 실패");
            }
        }
    }
}
