package com.book.core.coupon.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.book.core.coupon.application.command.GetCouponsCommand;
import com.book.core.coupon.application.result.CouponResult;
import com.book.core.coupon.application.usecase.GetCouponTargetsUseCase;
import com.book.core.coupon.application.usecase.GetCouponsUseCase;
import java.util.List;
import org.junit.jupiter.api.Test;

class GetCouponsServiceTest {
    @Test
    void 일반_쿠폰_조회는_상품_대상을_다시_조회하지_않고_ID_목록을_전달한다() {
        final var targets = mock(GetCouponTargetsUseCase.class);
        final var coupons = mock(GetCouponsUseCase.class);
        final var service = new CouponService(targets, coupons);
        final var command = new GetCouponsCommand(List.of(10L, 20L));
        final List<CouponResult> result = List.of();
        when(coupons.execute(command.couponIds())).thenReturn(result);

        assertThat(service.getCoupons(command)).isSameAs(result);
        verify(coupons).execute(command.couponIds());
        verifyNoInteractions(targets);
    }
}
