package com.book.core.coupon.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.inOrder;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.book.core.coupon.application.result.OwnedCouponResult;
import com.book.core.coupon.application.usecase.IssueCouponUseCase;
import com.book.core.coupon.application.usecase.FindOwnedCouponsForCheckoutUseCase;
import com.book.core.coupon.application.usecase.GetCouponTargetsUseCase;
import com.book.core.coupon.application.usecase.GetOwnedCouponsUseCase;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class CouponServiceTest {
    private final GetOwnedCouponsUseCase ownedCouponReader = mock(GetOwnedCouponsUseCase.class);
    private final IssueCouponUseCase issueCouponUseCase = mock(IssueCouponUseCase.class);
    private final GetCouponTargetsUseCase couponTargetReader = mock(GetCouponTargetsUseCase.class);
    private final FindOwnedCouponsForCheckoutUseCase checkoutReader = mock(FindOwnedCouponsForCheckoutUseCase.class);
    private final OwnedCouponService service =
        new OwnedCouponService(ownedCouponReader, issueCouponUseCase, couponTargetReader, checkoutReader);

    @Test
    void 체크아웃은_대상_조회_후_인증_회원과_기준_시간으로_보유_쿠폰을_조회한다() {
        when(couponTargetReader.execute(List.of(100L))).thenReturn(Set.of(10L));
        final List<OwnedCouponResult> result = List.of();
        when(checkoutReader.execute(eq(42L), eq(Set.of(10L)), any(LocalDateTime.class))).thenReturn(result);
        assertThat(service.getOwnedCouponsForCheckout(42L, List.of(100L))).isSameAs(result);
        final var order = inOrder(couponTargetReader, checkoutReader);
        order.verify(couponTargetReader).execute(List.of(100L));
        order.verify(checkoutReader).execute(eq(42L), eq(Set.of(10L)), any(LocalDateTime.class));
    }

    @Test
    void 체크아웃_상품이나_적용_쿠폰이_없으면_다음_조회를_생략한다() {
        assertThat(service.getOwnedCouponsForCheckout(42L, List.of())).isEmpty();
        verifyNoInteractions(couponTargetReader, checkoutReader);
        when(couponTargetReader.execute(List.of(100L))).thenReturn(Set.of());
        assertThat(service.getOwnedCouponsForCheckout(42L, List.of(100L))).isEmpty();
        verifyNoInteractions(checkoutReader);
    }

}
