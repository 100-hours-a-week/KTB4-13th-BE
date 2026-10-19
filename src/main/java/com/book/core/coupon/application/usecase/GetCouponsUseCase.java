package com.book.core.coupon.application.usecase;

import com.book.common.annotation.UseCase;
import com.book.core.coupon.application.port.CouponRepositoryPort;
import com.book.core.coupon.application.result.CouponResult;
import java.util.Collection;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

import static java.util.Collections.emptyList;

@UseCase
@RequiredArgsConstructor
public class GetCouponsUseCase {
    private final CouponRepositoryPort couponRepository;

    @Transactional(readOnly = true)
    public List<CouponResult> execute(final Collection<Long> couponIds) {
        if (couponIds.isEmpty()) {
            return emptyList();
        }
        return couponRepository.findActiveByIds(couponIds).stream().map(CouponResult::from).toList();
    }
}
