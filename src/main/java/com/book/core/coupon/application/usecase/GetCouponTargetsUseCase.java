package com.book.core.coupon.application.usecase;

import com.book.common.annotation.UseCase;
import com.book.core.coupon.application.port.CouponTargetRepositoryPort;
import com.book.core.coupon.domain.CouponTarget;
import com.book.core.coupon.domain.enums.CouponTargetType;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

import static java.util.Collections.emptySet;

@UseCase
@RequiredArgsConstructor
public class GetCouponTargetsUseCase {
    private final CouponTargetRepositoryPort couponTargetRepository;

    @Transactional(readOnly = true)
    public Set<Long> execute(final Collection<Long> productIds) {
        if (productIds.isEmpty()) {
            return emptySet();
        }
        final List<CouponTarget> productTargets =
            couponTargetRepository.findActiveByTargetTypeAndTargetIds(CouponTargetType.PRODUCT, productIds);
        final List<Long> categoryIds = couponTargetRepository.findCategoryIdsByProductIds(productIds);
        final HashSet<Long> couponIds = new HashSet<>();
        productTargets.forEach((final var target) -> couponIds.add(target.couponId()));
        if (!categoryIds.isEmpty()) {
            final List<CouponTarget> categoryTargets =
                couponTargetRepository.findActiveByTargetTypeAndTargetIds(CouponTargetType.PRODUCT_CATEGORY, categoryIds);
            categoryTargets.forEach((final var target) -> couponIds.add(target.couponId()));
        }
        return Set.copyOf(couponIds);
    }
}
