package com.book.core.coupon.application.usecase;

import com.book.common.annotation.UseCase;
import com.book.core.coupon.application.port.SignupCouponReceiptRepositoryPort;
import java.time.Clock;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
public class PurgeSignupCouponReceiptsUseCase {
    private final SignupCouponReceiptRepositoryPort receiptRepository;
    private final Clock clock;

    @Transactional
    public int execute() {
        return receiptRepository.deleteExpiredBefore(LocalDateTime.now(clock));
    }
}
