package com.book.core.coupon.application.usecase;

import com.book.common.annotation.UseCase;
import com.book.core.coupon.application.port.CouponRepositoryPort;
import com.book.core.coupon.application.port.OwnedCouponRepositoryPort;
import com.book.core.coupon.application.result.OwnedCouponResult;
import com.book.core.coupon.domain.Coupon;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.book.core.coupon.domain.OwnedCoupon;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

import static java.util.Collections.emptyList;

@UseCase
@RequiredArgsConstructor
public class GetOwnedCouponsUseCase {
    private final OwnedCouponRepositoryPort ownedCouponRepository;
    private final CouponRepositoryPort couponRepository;

    @Transactional(readOnly = true)
    public List<OwnedCouponResult> execute(final Long userId) {
        final List<OwnedCoupon> owned = ownedCouponRepository.findActiveByUserId(userId);
        if (owned.isEmpty()) {
            return emptyList();
        }

        final Set<Long> couponIds = owned.stream().map(OwnedCoupon::couponId).collect(Collectors.toSet());
        final Map<Long, Coupon> couponMap =
            couponRepository.findAllById(couponIds).stream().collect(Collectors.toMap(Coupon::id, Function.identity()));
        return owned.stream().map((final var item) -> OwnedCouponResult.from(item, couponMap.get(item.couponId()))).toList();
    }
}
