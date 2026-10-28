package com.book.core.coupon.infrastructure.persistence.repository;

import com.book.core.coupon.application.port.OwnedCouponRepositoryPort;
import com.book.core.coupon.domain.OwnedCoupon;
import com.book.core.coupon.domain.enums.OwnedCouponStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class OwnedCouponRepositoryAdapter implements OwnedCouponRepositoryPort {
    private final OwnedCouponJpaRepository ownedCouponRepository;
    private final JdbcTemplate jdbcTemplate;

    @Override
    public Optional<OwnedCoupon> findByUserIdAndCouponId(final Long userId, final Long couponId) {
        return ownedCouponRepository.findByUserIdAndCouponId(userId, couponId);
    }

    @Override
    public boolean issueIfAbsent(final OwnedCoupon ownedCoupon) {
        // MySQL returns the new generated ID, or zero for the duplicate branch, on this transaction-bound
        // connection.
        jdbcTemplate.update("""
            INSERT INTO owned_coupon (user_id, coupon_id, state, max_use_count, used_count, created_at, updated_at)
            VALUES (?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
            ON DUPLICATE KEY UPDATE id = id + LAST_INSERT_ID(0)
            """, ownedCoupon.userId(), ownedCoupon.couponId(), ownedCoupon.status().name(), ownedCoupon.maxUseCount(),
            ownedCoupon.usedCount());
        return jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class) != 0L;
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
