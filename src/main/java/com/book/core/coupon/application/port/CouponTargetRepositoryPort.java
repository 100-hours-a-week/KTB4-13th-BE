package com.book.core.coupon.application.port;

import com.book.core.coupon.domain.CouponTarget;
import com.book.core.coupon.domain.enums.CouponTargetType;
import java.util.Collection;
import java.util.List;

public interface CouponTargetRepositoryPort {
    List<CouponTarget> findActiveByTargetTypeAndTargetIds(final CouponTargetType targetType, final Collection<Long> targetIds);

    List<Long> findCategoryIdsByProductIds(final Collection<Long> productIds);
}
