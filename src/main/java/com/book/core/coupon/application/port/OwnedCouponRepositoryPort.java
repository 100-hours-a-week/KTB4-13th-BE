package com.book.core.coupon.application.port;

import com.book.core.coupon.domain.OwnedCoupon;
import java.util.List;
import java.util.Optional;

public interface OwnedCouponRepositoryPort {
    Optional<OwnedCoupon> findByUserIdAndCouponId(final Long userId, final Long couponId);

    List<OwnedCoupon> findActiveByUserId(final Long userId);

    Optional<OwnedCoupon> findById(final Long ownedCouponId);


    OwnedCoupon save(final OwnedCoupon ownedCoupon);
}
