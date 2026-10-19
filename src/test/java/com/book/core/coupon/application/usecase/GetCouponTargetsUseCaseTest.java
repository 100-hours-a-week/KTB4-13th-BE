package com.book.core.coupon.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.book.core.coupon.application.port.CouponTargetRepositoryPort;
import com.book.core.coupon.domain.CouponTarget;
import com.book.core.coupon.domain.enums.CouponTargetType;
import java.util.List;
import org.junit.jupiter.api.Test;

class GetCouponTargetsUseCaseTest {
    private final CouponTargetRepositoryPort couponTargetRepository = mock(CouponTargetRepositoryPort.class);

    @Test
    void 상품과_카테고리_대상의_쿠폰_ID는_중복을_제거한다() {
        when(couponTargetRepository.findActiveByTargetTypeAndTargetIds(CouponTargetType.PRODUCT, List.of(100L)))
            .thenReturn(List.of(new CouponTarget(1L, 10L, CouponTargetType.PRODUCT, 100L)));
        when(couponTargetRepository.findCategoryIdsByProductIds(List.of(100L))).thenReturn(List.of(200L));
        when(couponTargetRepository.findActiveByTargetTypeAndTargetIds(CouponTargetType.PRODUCT_CATEGORY, List.of(200L)))
            .thenReturn(List.of(new CouponTarget(2L, 10L, CouponTargetType.PRODUCT_CATEGORY, 200L),
                new CouponTarget(3L, 11L, CouponTargetType.PRODUCT_CATEGORY, 200L)));
        assertThat(new GetCouponTargetsUseCase(couponTargetRepository).execute(List.of(100L))).containsExactlyInAnyOrder(10L, 11L);
    }

    @Test
    void 상품이나_카테고리가_없으면_대상_조회를_생략한다() {
        final var useCase = new GetCouponTargetsUseCase(couponTargetRepository);
        assertThat(useCase.execute(List.of())).isEmpty();
        verifyNoInteractions(couponTargetRepository);
        when(couponTargetRepository.findCategoryIdsByProductIds(List.of(100L))).thenReturn(List.of());
        when(couponTargetRepository.findActiveByTargetTypeAndTargetIds(CouponTargetType.PRODUCT, List.of(100L))).thenReturn(List.of());
        assertThat(useCase.execute(List.of(100L))).isEmpty();
        verify(couponTargetRepository, never()).findActiveByTargetTypeAndTargetIds(CouponTargetType.PRODUCT_CATEGORY, List.of());
    }
}
