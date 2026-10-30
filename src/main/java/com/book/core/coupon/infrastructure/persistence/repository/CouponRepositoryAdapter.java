package com.book.core.coupon.infrastructure.persistence.repository;

import com.book.core.coupon.application.port.CouponRepositoryPort;
import com.book.core.coupon.domain.Coupon;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class CouponRepositoryAdapter implements CouponRepositoryPort {
    private final CouponJpaRepository couponRepository;
    private final EntityManager entityManager;

    @Override
    public List<Coupon> findActiveByIds(final Collection<Long> couponIds) {
        return couponRepository.findByIdInAndDeletedAtIsNull(couponIds);
    }

    @Override
    public Optional<Coupon> findByIdWithLock(final Long couponId) {

        final Coupon coupon = entityManager.find(Coupon.class, couponId);
        if (coupon == null) {
            return couponRepository.findByIdAndDeletedAtIsNullForUpdate(couponId);
        }
        if (entityManager.getLockMode(coupon) != LockModeType.PESSIMISTIC_WRITE) {
            entityManager.refresh(coupon, LockModeType.PESSIMISTIC_WRITE);
        }
        return coupon.isDeleted() ? Optional.empty() : Optional.of(coupon);
    }

    @Override
    public Optional<Coupon> findById(final Long couponId) {
        return couponRepository.findById(couponId);
    }

    @Override
    public List<Coupon> findAllById(final Collection<Long> couponIds) {
        return couponRepository.findAllById(couponIds);
    }
}
