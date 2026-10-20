package com.book.core.coupon.infrastructure.persistence.repository;

import com.book.core.coupon.application.port.SignupCouponReceiptRepositoryPort;
import com.book.core.coupon.domain.SignupCouponReceipt;
import java.util.Optional;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
class SignupCouponReceiptRepositoryAdapter implements SignupCouponReceiptRepositoryPort {
    private final SignupCouponReceiptJpaRepository jpaRepository;

    @Override
    public Optional<SignupCouponReceipt> findByCouponIdAndProviderEmailForUpdate(final Long couponId, final String providerEmail) {
        return jpaRepository.findByCouponIdAndProviderEmail(couponId, providerEmail.strip().toLowerCase(java.util.Locale.ROOT));
    }

    @Override
    public Optional<SignupCouponReceipt> findByCouponIdAndUserIdForUpdate(final Long couponId, final Long userId) {
        return jpaRepository.findByCouponIdAndUserId(couponId, userId);
    }

    @Override
    public SignupCouponReceipt save(final SignupCouponReceipt receipt) {
        return jpaRepository.save(receipt);
    }

    @Override
    public int deleteExpiredBefore(final LocalDateTime now) {
        return jpaRepository.deleteExpiredBefore(now);
    }
}
