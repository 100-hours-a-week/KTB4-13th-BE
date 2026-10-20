package com.book.core.coupon.application.port;

import com.book.core.coupon.domain.SignupCouponReceipt;
import java.util.Optional;
import java.time.LocalDateTime;

public interface SignupCouponReceiptRepositoryPort {
    Optional<SignupCouponReceipt> findByCouponIdAndProviderEmailForUpdate(final Long couponId, final String providerEmail);

    Optional<SignupCouponReceipt> findByCouponIdAndUserIdForUpdate(final Long couponId, final Long userId);

    SignupCouponReceipt save(final SignupCouponReceipt receipt);

    int deleteExpiredBefore(final LocalDateTime now);
}
