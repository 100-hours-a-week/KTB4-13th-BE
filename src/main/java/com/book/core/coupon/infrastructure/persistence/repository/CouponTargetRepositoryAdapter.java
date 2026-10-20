package com.book.core.coupon.infrastructure.persistence.repository;

import com.book.core.coupon.application.port.CouponTargetRepositoryPort;
import com.book.core.coupon.domain.CouponTarget;
import com.book.core.coupon.domain.enums.CouponTargetType;
import java.util.Collection;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class CouponTargetRepositoryAdapter implements CouponTargetRepositoryPort {
    private final CouponTargetJpaRepository couponTargetRepository;

    @Override
    public List<CouponTarget> findActiveByTargetTypeAndTargetIds(final CouponTargetType targetType, final Collection<Long> targetIds) {
        return couponTargetRepository.findByTargetTypeAndTargetIdInAndDeletedAtIsNull(targetType, targetIds);
    }

    @Override
    public List<Long> findCategoryIdsByProductIds(final Collection<Long> productIds) {
        return couponTargetRepository.findCategoryIdsByProductIds(productIds);
    }
}
