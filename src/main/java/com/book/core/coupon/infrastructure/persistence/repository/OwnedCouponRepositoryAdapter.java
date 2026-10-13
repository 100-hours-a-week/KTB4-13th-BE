package com.book.core.coupon.infrastructure.persistence.repository;

import com.book.core.coupon.application.port.OwnedCouponRepositoryPort;
import com.book.core.coupon.domain.OwnedCoupon;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class OwnedCouponRepositoryAdapter implements OwnedCouponRepositoryPort {
    private final OwnedCouponJpaRepository ownedCouponRepository;

    @Override
    public Optional<OwnedCoupon> findByUserIdAndCouponId(final Long userId, final Long couponId) {
        return ownedCouponRepository.findByUserIdAndCouponId(userId, couponId);
    }

    @Override
    public List<OwnedCoupon> findActiveByUserId(final Long userId) {
        return ownedCouponRepository.findByUserIdAndDeletedAtIsNull(userId);
    }

    @Override
    public Optional<OwnedCoupon> findById(final Long ownedCouponId) {
        return ownedCouponRepository.findById(ownedCouponId);
    }



    @Override
    public OwnedCoupon save(final OwnedCoupon ownedCoupon) {
        return ownedCouponRepository.save(ownedCoupon);
    }
}
