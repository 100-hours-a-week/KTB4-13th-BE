package com.book.core.coupon.application.port;

import com.book.core.coupon.domain.SignupCouponQuota;
import java.util.Optional;

public interface SignupCouponQuotaRepositoryPort {
    Optional<SignupCouponQuota> findByCouponIdForUpdate(final Long couponId);
}
