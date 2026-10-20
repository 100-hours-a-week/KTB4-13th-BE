package com.book.core.coupon.infrastructure.persistence.repository;

import com.book.core.coupon.application.port.SignupCouponQuotaRepositoryPort;
import com.book.core.coupon.domain.SignupCouponQuota;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
class SignupCouponQuotaRepositoryAdapter implements SignupCouponQuotaRepositoryPort {
    private final SignupCouponQuotaJpaRepository jpaRepository;

    @Override
    public Optional<SignupCouponQuota> findByCouponIdForUpdate(final Long couponId) {
        return jpaRepository.findByCouponId(couponId);
    }
}
