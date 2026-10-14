package com.book.core.coupon.application.port;

import com.book.core.coupon.domain.OwnedCoupon;
import java.util.Optional;

public interface OwnedCouponRepositoryPort {
    Optional<OwnedCoupon> findByUserIdAndCouponId(final Long userId, final Long couponId);



    OwnedCoupon save(final OwnedCoupon ownedCoupon);
}
