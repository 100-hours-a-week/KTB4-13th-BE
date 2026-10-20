package com.book.core.coupon.infrastructure.scheduler;

import com.book.core.coupon.application.usecase.PurgeSignupCouponReceiptsUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SignupCouponReceiptScheduler {
    private final PurgeSignupCouponReceiptsUseCase purgeSignupCouponReceiptsUseCase;

    @Scheduled(cron = "0 30 3 * * *", zone = "Asia/Seoul")
    public void purgeExpiredReceipts() {
        purgeSignupCouponReceiptsUseCase.execute();
    }
}
