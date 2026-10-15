package com.book.core.order.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import com.book.core.order.application.port.OrderRepositoryPort;
import com.book.core.order.application.usecase.GetPaymentOrderUseCase;
import com.book.core.order.domain.Order;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class PaymentOrderUseCasesTest {
    private final OrderRepositoryPort orders = mock(OrderRepositoryPort.class);

    @Test
    void 일반_결제는_CREATED와_소유자를_확인한다() {
        final var order = Order.create(1L, "order", null);
        order.addItem(10L, "책", "", "저자", BigDecimal.TEN, BigDecimal.TEN, 1);
        when(orders.findActiveOrderByKey("order")).thenReturn(Optional.of(order));
        final var useCase = new GetPaymentOrderUseCase(orders);
        assertThat(useCase.execute(1L, "order")).isSameAs(order);
        assertThatThrownBy(() -> useCase.execute(2L, "order")).isInstanceOf(CoreException.class);
        order.cancel();
        assertThatThrownBy(() -> useCase.execute(1L, "order")).isInstanceOf(CoreException.class);
    }

    @Test
    void 상품이_없는_주문은_결제_생성_대상에서_제외한다() {
        final var order = Order.create(1L, "empty-order", null);
        when(orders.findActiveOrderByKey("empty-order")).thenReturn(Optional.of(order));
        final var useCase = new GetPaymentOrderUseCase(orders);

        assertThatThrownBy(() -> useCase.execute(1L, "empty-order")).isInstanceOfSatisfying(CoreException.class,
            (final var exception) -> assertThat(exception.errorCode()).isEqualTo(ErrorCode.NOT_FOUND_DATA));
    }
}
