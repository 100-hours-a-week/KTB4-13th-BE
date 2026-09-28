package com.book.core.order.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.book.core.order.application.port.OrderRepositoryPort;
import com.book.core.order.application.result.CreateOrderItemResult;
import com.book.core.order.application.result.CreateOrderResult;
import com.book.core.order.application.usecase.CreateOrderUseCase;
import com.book.core.order.domain.Order;
import com.book.core.order.domain.OrderAddress;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;

class CreateOrderUseCaseTest {
    private final OrderRepositoryPort orderRepository = mock(OrderRepositoryPort.class);
    private final CreateOrderUseCase useCase = new CreateOrderUseCase(orderRepository);

    @Test
    void 주문을_저장하고_주문상품_스냅샷과_결제_가능_여부를_반환한다() {
        final Order order = order("order_with_address", OrderAddress.from("06236", "서울 주소", "101호"));
        when(orderRepository.save(order)).thenReturn(order);

        final CreateOrderResult result = useCase.execute(order);

        assertThat(result.orderKey()).isEqualTo("order_with_address");
        assertThat(result.canProceedToPayment()).isTrue();
        assertThat(result.totalPrice()).isEqualByComparingTo("34.50");
        assertThat(result.items()).containsExactly(new CreateOrderItemResult(701L, "상품 701", null, "저자", new BigDecimal("20.00"),
            new BigDecimal("17.25"), 2, new BigDecimal("34.50")));
        verify(orderRepository).save(order);
    }

    @Test
    void 배송지가_없는_주문을_저장하고_결제_불가로_표시한다() {
        final Order order = order("order_without_address", null);
        when(orderRepository.save(order)).thenReturn(order);

        final CreateOrderResult result = useCase.execute(order);

        assertThat(result.orderKey()).isEqualTo("order_without_address");
        assertThat(result.canProceedToPayment()).isFalse();
        assertThat(result.totalPrice()).isEqualByComparingTo("34.50");
        verify(orderRepository).save(order);
    }

    @Test
    void 저장_UseCase는_쓰기_트랜잭션에서_실행한다() throws NoSuchMethodException {
        final var method = CreateOrderUseCase.class.getMethod("execute", Order.class);

        assertThat(method.getAnnotation(Transactional.class)).isNotNull();
        assertThat(method.getAnnotation(Transactional.class).readOnly()).isFalse();
    }

    private static Order order(final String orderKey, final OrderAddress address) {
        final Order order = Order.create(42L, orderKey, address);
        order.addItem(701L, "상품 701", null, "저자", new BigDecimal("20.00"), new BigDecimal("17.25"), 2);
        return order;
    }
}
