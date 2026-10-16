package com.book.core.payment.application.usecase;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.book.core.cart.application.usecase.DeleteCartItemsByProductsUseCase;
import com.book.core.order.application.usecase.GetPaymentOrderUseCase;
import com.book.core.order.domain.Order;
import com.book.core.order.domain.OrderStatus;
import com.book.core.payment.application.port.PaymentRepositoryPort;
import com.book.core.payment.domain.Payment;
import com.book.core.payment.domain.enums.PaymentMethod;
import com.book.core.payment.domain.enums.PaymentState;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class PostProcessPaymentUseCaseTest {
    private final PaymentRepositoryPort payments = mock(PaymentRepositoryPort.class);
    private final GetPaymentOrderUseCase orders = mock(GetPaymentOrderUseCase.class);
    private final DeleteCartItemsByProductsUseCase carts = mock(DeleteCartItemsByProductsUseCase.class);
    private final PostProcessPaymentUseCase post = new PostProcessPaymentUseCase(payments, orders, carts);

    @Test
    void 성공한_주문의_상품만_주문_소유자_장바구니에서_제거한다() {
        final Payment payment =
            new Payment(1L, 2L, BigDecimal.TEN, -1L, BigDecimal.ZERO, BigDecimal.ZERO, 99L, BigDecimal.TEN, PaymentState.READY);
        payment.success("test-key", PaymentMethod.CARD, "test");
        final Order order = Order.create(1L, "order", null);
        order.addItem(10L, "책", "", "저자", BigDecimal.TEN, BigDecimal.TEN, 1);
        order.paid();
        when(payments.findById(3L)).thenReturn(Optional.of(payment));
        when(orders.execute("order", OrderStatus.PAID)).thenReturn(order);
        post.execute(3L, "order");
        verify(carts).execute(1L, List.of(10L));
    }

    @Test
    void 주문_소유자가_결제_소유자와_다르면_장바구니를_변경하지_않는다() {
        final Payment payment =
            new Payment(1L, 2L, BigDecimal.TEN, -1L, BigDecimal.ZERO, BigDecimal.ZERO, 1L, BigDecimal.TEN, PaymentState.READY);
        payment.success("test-key", PaymentMethod.CARD, "test");
        final Order order = Order.create(2L, "other-order", null);
        order.addItem(10L, "책", "", "저자", BigDecimal.TEN, BigDecimal.TEN, 1);
        order.paid();
        when(payments.findById(3L)).thenReturn(Optional.of(payment));
        when(orders.execute("other-order", OrderStatus.PAID)).thenReturn(order);
        assertThatCode(() -> post.execute(3L, "other-order")).doesNotThrowAnyException();
        verifyNoInteractions(carts);
    }

    @Test
    void 없는_결제의_후처리_실패는_호출자에게_전파하지_않는다() {
        when(payments.findById(3L)).thenReturn(Optional.empty());
        assertThatCode(() -> post.execute(3L, "order")).doesNotThrowAnyException();
        verifyNoInteractions(orders, carts);
    }

    @Test
    void READY_결제는_장바구니를_변경하지_않는다() {
        final Payment payment =
            new Payment(1L, 2L, BigDecimal.TEN, -1L, BigDecimal.ZERO, BigDecimal.ZERO, 1L, BigDecimal.TEN, PaymentState.READY);
        when(payments.findById(3L)).thenReturn(Optional.of(payment));
        post.execute(3L, "order");
        verifyNoInteractions(orders, carts);
    }
}
