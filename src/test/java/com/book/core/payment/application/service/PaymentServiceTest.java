package com.book.core.payment.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.book.core.coupon.application.usecase.FindOwnedCouponsForCheckoutUseCase;
import com.book.core.coupon.application.usecase.GetCouponTargetsUseCase;
import com.book.core.order.application.usecase.GetPaymentOrderUseCase;
import com.book.core.order.domain.Order;
import com.book.core.order.domain.OrderStatus;
import com.book.core.payment.application.command.ApprovePaymentCommand;
import com.book.core.payment.application.command.CreatePaymentCommand;
import com.book.core.payment.application.usecase.CompletePaymentUseCase;
import com.book.core.payment.application.usecase.CreatePaymentUseCase;
import com.book.core.payment.application.usecase.PostProcessPaymentUseCase;
import com.book.core.payment.application.usecase.ValidatePaymentUseCase;
import com.book.core.payment.domain.PaymentDiscount;
import com.book.core.point.application.usecase.GetPointBalanceUseCase;
import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

class PaymentServiceTest {
    private final GetPaymentOrderUseCase orders = mock(GetPaymentOrderUseCase.class);
    private final GetCouponTargetsUseCase targets = mock(GetCouponTargetsUseCase.class);
    private final FindOwnedCouponsForCheckoutUseCase coupons = mock(FindOwnedCouponsForCheckoutUseCase.class);
    private final GetPointBalanceUseCase balance = mock(GetPointBalanceUseCase.class);
    private final CreatePaymentUseCase create = mock(CreatePaymentUseCase.class);
    private final ValidatePaymentUseCase validate = mock(ValidatePaymentUseCase.class);
    private final CompletePaymentUseCase complete = mock(CompletePaymentUseCase.class);
    private final PostProcessPaymentUseCase post = mock(PostProcessPaymentUseCase.class);
    private final PaymentService service = new PaymentService(orders, targets, coupons, balance, create, validate, complete, post);

    @Test
    void 일반_결제는_주문과_체크아웃_후보_잔액을_조합하고_원값을_전달한다() {
        final Order order = order();
        when(orders.execute(1L, "order")).thenReturn(order);
        when(targets.execute(List.of())).thenReturn(Set.of());
        when(coupons.execute(eq(1L), eq(Set.of()), any())).thenReturn(List.of());
        when(balance.execute(1L)).thenReturn(BigDecimal.ZERO);
        when(create.execute(eq(order), eq(1L), any())).thenReturn(3L);
        assertThat(service.create(new CreatePaymentCommand(1L, "order", -1L, BigDecimal.valueOf(-1)))).isEqualTo(3L);
        final ArgumentCaptor<PaymentDiscount> captured = ArgumentCaptor.forClass(PaymentDiscount.class);
        verify(create).execute(eq(order), eq(1L), captured.capture());
        assertThat(captured.getValue().paidAmount(BigDecimal.valueOf(10000))).isEqualByComparingTo("10001");
    }

    @Test
    void 성공은_검증_완료_후처리_순서로_실행한다() {
        when(orders.execute("order", OrderStatus.CREATED)).thenReturn(order());
        when(complete.execute("order", "test-key")).thenReturn(3L);
        assertThat(service.success(new ApprovePaymentCommand("order", "test-key", BigDecimal.TEN))).isEqualTo(3L);
        final var sequence = inOrder(validate, complete, post);
        sequence.verify(validate).validateForApprove(2L, BigDecimal.TEN);
        sequence.verify(complete).execute("order", "test-key");
        sequence.verify(post).execute(3L, "order");
    }

    @Test
    void 승인_검증_실패는_완료와_후처리를_실행하지_않는다() {
        when(orders.execute("order", OrderStatus.CREATED)).thenReturn(order());
        doThrow(new IllegalStateException("test")).when(validate).validateForApprove(2L, BigDecimal.TEN);
        assertThatThrownBy(() -> service.success(new ApprovePaymentCommand("order", "test-key", BigDecimal.TEN)))
            .isInstanceOf(IllegalStateException.class);
        verifyNoInteractions(complete, post);
    }

    private static Order order() {
        final Order order = Order.create(1L, "order", null);
        ReflectionTestUtils.setField(order, "id", 2L);
        return order;
    }
}
