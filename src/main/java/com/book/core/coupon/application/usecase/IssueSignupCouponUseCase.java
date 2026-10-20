package com.book.core.coupon.application.usecase;

import com.book.common.annotation.UseCase;
import com.book.core.coupon.application.port.CouponRepositoryPort;
import com.book.core.coupon.application.port.SignupCouponQuotaRepositoryPort;
import com.book.core.coupon.application.port.SignupCouponReceiptRepositoryPort;
import com.book.core.coupon.domain.SignupCouponReceipt;
import java.time.Clock;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
public class IssueSignupCouponUseCase {
    private static final Long SIGNUP_COUPON_ID = 1L;

    private final SignupCouponQuotaRepositoryPort quotaRepository;
    private final SignupCouponReceiptRepositoryPort receiptRepository;
    private final CouponRepositoryPort couponRepository;
    private final IssueCouponUseCase issueCouponUseCase;
    private final Clock clock;

    @Transactional
    public boolean execute(final Long userId, final String providerEmail) {
        if (providerEmail == null || providerEmail.isBlank()) {
            return false;
        }
        final LocalDateTime now = LocalDateTime.now(clock);
        final var quota = quotaRepository.findByCouponIdForUpdate(SIGNUP_COUPON_ID).orElse(null);
        if (quota == null || !quota.canIssue()) {
            return false;
        }
        if (couponRepository.findIssuableById(SIGNUP_COUPON_ID, now).isEmpty()) {
            return false;
        }
        final String email = providerEmail.strip().toLowerCase(java.util.Locale.ROOT);
        final var existingReceipt = receiptRepository.findByCouponIdAndProviderEmailForUpdate(SIGNUP_COUPON_ID, email);
        if (existingReceipt.isPresent() && existingReceipt.orElseThrow().isHeld(now)) {
            return false;
        }
        issueCouponUseCase.execute(userId, SIGNUP_COUPON_ID);
        if (existingReceipt.isPresent()) {
            existingReceipt.orElseThrow().reissue(userId, now);
        } else {
            receiptRepository.save(new SignupCouponReceipt(SIGNUP_COUPON_ID, email, userId, now));
        }
        quota.issue();
        return true;
    }
}
