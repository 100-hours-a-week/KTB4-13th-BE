package com.book.core.coupon.infrastructure.persistence.repository;

import com.book.core.coupon.application.port.OwnedCouponRepositoryPort;
import com.book.core.coupon.domain.OwnedCoupon;
import com.book.core.coupon.domain.enums.OwnedCouponStatus;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class OwnedCouponRepositoryAdapter implements OwnedCouponRepositoryPort {
    private final OwnedCouponJpaRepository ownedCouponRepository;
    private final OwnedCouponQueryRepository queryRepository;

    @Override
    public Optional<OwnedCoupon> findByUserIdAndCouponId(final Long userId, final Long couponId) {
        return ownedCouponRepository.findByUserIdAndCouponId(userId, couponId);
    }

    @Override
    public boolean existsByUserIdAndCouponId(final Long userId, final Long couponId) {
        return queryRepository.existsByUserIdAndCouponId(userId, couponId);
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
    public List<OwnedCoupon> findOwnedCouponIds(final Long userId, final Collection<Long> couponIds, final LocalDateTime expiredAtAfter) {
        return ownedCouponRepository.findOwnedCouponIds(userId, couponIds, expiredAtAfter, OwnedCouponStatus.ISSUED);
    }

    @Override
    public OwnedCoupon save(final OwnedCoupon ownedCoupon) {
        return ownedCouponRepository.save(ownedCoupon);
    }
}
