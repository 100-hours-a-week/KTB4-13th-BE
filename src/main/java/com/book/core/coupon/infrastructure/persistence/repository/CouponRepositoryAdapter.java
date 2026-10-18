package com.book.core.coupon.infrastructure.persistence.repository;

import com.book.core.coupon.application.port.CouponRepositoryPort;
import com.book.core.coupon.domain.Coupon;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class CouponRepositoryAdapter implements CouponRepositoryPort {
    private final CouponJpaRepository couponRepository;

    @Override
    public List<Coupon> findActiveByIds(final Collection<Long> couponIds) {
        return couponRepository.findByIdInAndDeletedAtIsNull(couponIds);
    }

    @Override
    public Optional<Coupon> findIssuableById(final Long couponId, final LocalDateTime expiredAtAfter) {
        return couponRepository.findByIdAndDeletedAtIsNullAndExpiredAtAfter(couponId, expiredAtAfter);
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
