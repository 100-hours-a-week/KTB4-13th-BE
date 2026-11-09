package com.book.core.coupon.infrastructure.persistence.repository;

import com.book.core.coupon.application.port.CouponIssuanceCounts;
import com.book.core.coupon.application.port.CouponRepositoryPort;
import com.book.core.coupon.domain.Coupon;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@RequiredArgsConstructor
public class CouponRepositoryAdapter implements CouponRepositoryPort {
    private final CouponJpaRepository couponRepository;
    private final CouponQueryRepository queryRepository;

    @Override
    public List<Coupon> findActiveByIds(final Collection<Long> couponIds) {
        return couponRepository.findByIdInAndDeletedAtIsNull(couponIds);
    }

    @Override
    public Optional<Coupon> findById(final Long couponId) {
        return couponRepository.findByIdAndDeletedAtIsNull(couponId);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<CouponIssuanceCounts> findIssuanceCounts(final long couponId) {
        return queryRepository.findIssuanceCounts(couponId);
    }

    @Override
    public Optional<Coupon> findByIdForUpdate(final Long couponId) {
        return couponRepository.findByIdForUpdate(couponId);
    }

    @Override
    public List<Coupon> findAllById(final Collection<Long> couponIds) {
        return couponRepository.findAllById(couponIds);
    }

    @Override
    public Coupon save(final Coupon coupon) {
        return couponRepository.save(coupon);
    }

    @Override
    public int incrementIssuedQuantity(final Long couponId) {
        return couponRepository.incrementIssuedQuantity(couponId);
    }
}
