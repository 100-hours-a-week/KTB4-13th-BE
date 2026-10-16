package com.book.core.cancel.application.usecase;

import com.book.core.cancel.domain.CancelCalculated;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.book.common.exception.CoreException;
import com.book.core.cancel.application.port.CancelBalanceRepositoryPort;
import com.book.core.cancel.domain.CancelBalance;
import com.book.core.cancel.domain.PartialCancelAction;
import com.book.core.coupon.application.result.CouponResult;
import com.book.core.coupon.application.result.OwnedCouponResult;
import com.book.core.coupon.application.usecase.GetOwnedCouponUseCase;
import com.book.core.coupon.domain.enums.CouponType;
import com.book.core.coupon.domain.enums.OwnedCouponStatus;
import com.book.core.order.application.usecase.GetOrderForCancelUseCase;
import com.book.core.order.application.usecase.GetOrderItemForCancelUseCase;
import com.book.core.order.domain.Order;
import com.book.core.payment.application.usecase.GetPaymentUseCase;
import com.book.core.payment.domain.Payment;
import com.book.core.payment.domain.enums.PaymentState;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class CalculatePartialCancelUseCaseTest {
    private final GetOrderForCancelUseCase orders = mock(GetOrderForCancelUseCase.class);
    private final GetOrderItemForCancelUseCase items = mock(GetOrderItemForCancelUseCase.class);
    private final GetPaymentUseCase payments = mock(GetPaymentUseCase.class);
    private final CancelBalanceRepositoryPort balances = mock(CancelBalanceRepositoryPort.class);
    private final GetOwnedCouponUseCase coupons = mock(GetOwnedCouponUseCase.class);
    private final CalculatePartialCancelUseCase useCase = new CalculatePartialCancelUseCase(orders, items, payments, balances, coupons);

    @Test
    void 단가와_취소수량으로_계산하고_쿠폰이_없으면_최소금액_조회는_생략한다() {
        prepare(-1L);
        final CancelCalculated result = useCase.execute(new PartialCancelAction("order", 4L, 2L));
        assertThat(result.paidAmount()).isEqualByComparingTo("15000");
        assertThat(result.pointAmount()).isEqualByComparingTo("5000");
        assertThat(result.isRestoreCoupon()).isFalse();
        verifyNoInteractions(coupons);
    }

    @Test
    void 사용한_쿠폰의_최소금액을_조회해서_복원_여부를_판단한다() {
        prepare(10L);
        when(coupons.execute(10L)).thenReturn(new OwnedCouponResult(10L, 1L, OwnedCouponStatus.USED, 1L, 1L, new CouponResult(20L, "쿠폰",
            CouponType.FIXED_AMOUNT, BigDecimal.valueOf(10000), BigDecimal.valueOf(25000), LocalDateTime.now())));
        when(balances.findByOrderId(2L)).thenReturn(Optional.of(new CancelBalance(2L, BigDecimal.valueOf(20000), BigDecimal.ZERO,
            BigDecimal.valueOf(10000), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO)));
        final CancelCalculated result = useCase.execute(new PartialCancelAction("order", 4L, 1L));
        assertThat(result.couponAmount()).isEqualByComparingTo("10000");
        assertThat(result.paidAmount()).isEqualByComparingTo("0");
        assertThat(result.isRestoreCoupon()).isTrue();
    }

    @Test
    void 취소잔액이_없으면_업무_오류를_전파한다() {
        prepare(-1L);
        when(balances.findByOrderId(2L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> useCase.execute(new PartialCancelAction("order", 4L, 1L))).isInstanceOf(CoreException.class);
    }

    private void prepare(final Long couponId) {
        final Order order = Order.create(1L, "order", null);
        ReflectionTestUtils.setField(order, "id", 2L);
        order.addItem(10L, "책", "", "저자", BigDecimal.valueOf(10000), BigDecimal.valueOf(10000), 3);
        when(orders.execute("order")).thenReturn(order);
        when(items.execute(4L)).thenReturn(order.items().getFirst());
        when(payments.execute(2L)).thenReturn(new Payment(1L, 2L, order.totalPrice(), couponId, BigDecimal.ZERO, BigDecimal.valueOf(15000),
            1L, BigDecimal.valueOf(15000), PaymentState.SUCCESS));
        when(balances.findByOrderId(2L)).thenReturn(Optional.of(new CancelBalance(2L, BigDecimal.valueOf(15000), BigDecimal.valueOf(15000),
            BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO)));
    }
}
