package com.book.core.coupon.application.usecase;

import com.book.common.annotation.UseCase;
import com.book.core.coupon.application.port.SignupCouponReceiptRepositoryPort;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
public class RetainSignupCouponReceiptUseCase {
    private static final Long SIGNUP_COUPON_ID = 1L;

    private final SignupCouponReceiptRepositoryPort receiptRepository;

    @Transactional
    public void execute(final Long userId, final LocalDateTime deletedAt) {
        receiptRepository.findByCouponIdAndUserIdForUpdate(SIGNUP_COUPON_ID, userId)
            .ifPresent((final var receipt) -> receipt.retainUntil(deletedAt.plusYears(1)));
    }
}
