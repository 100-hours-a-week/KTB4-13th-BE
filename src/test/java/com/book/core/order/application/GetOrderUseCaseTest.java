package com.book.core.order.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import com.book.core.order.application.command.GetOrderCommand;
import com.book.core.order.application.port.OrderRepositoryPort;
import com.book.core.order.application.usecase.GetOrderUseCase;
import com.book.core.order.domain.Order;
import com.book.core.order.domain.OrderAddress;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class GetOrderUseCaseTest {
    private final OrderRepositoryPort orderRepository = mock(OrderRepositoryPort.class);
    private final GetOrderUseCase useCase = new GetOrderUseCase(orderRepository);

    @Test
    void 회원의_주문과_활성_주문상품을_반환한다() {
        final Order order = order(42L, "order_detail");
        order.addItem(702L, "삭제 상품", null, "저자", BigDecimal.TEN, BigDecimal.ONE, 1);
        order.items().getLast().delete();
        when(orderRepository.findActiveOrderByKey("order_detail")).thenReturn(Optional.of(order));

        final var result = useCase.execute(new GetOrderCommand(42L, "order_detail"));

        assertThat(result.key()).isEqualTo("order_detail");
        assertThat(result.name()).isEqualTo("상품 701");
        assertThat(result.totalPrice()).isEqualByComparingTo("3.00");
        assertThat(result.address()).satisfies(address -> {
            assertThat(address.postalCode()).isEqualTo("06236");
            assertThat(address.address()).isEqualTo("서울 주소");
            assertThat(address.detailAddress()).isEqualTo("101호");
        });
        assertThat(result.items()).singleElement().satisfies(item -> {
            assertThat(item.productId()).isEqualTo(701L);
            assertThat(item.itemName()).isEqualTo("상품 701");
            assertThat(item.quantity()).isEqualTo(2);
        });
    }

    @Test
    void 존재하지_않는_주문은_찾을_수_없음으로_실패한다() {
        when(orderRepository.findActiveOrderByKey("missing_order")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(new GetOrderCommand(42L, "missing_order"))).isInstanceOf(CoreException.class)
            .extracting(exception -> ((CoreException) exception).errorCode()).isEqualTo(ErrorCode.ORDER_NOT_FOUND);
    }

    @Test
    void 다른_회원의_주문은_조회하지_않는다() {
        when(orderRepository.findActiveOrderByKey("another_order")).thenReturn(Optional.of(order(43L, "another_order")));

        assertThatThrownBy(() -> useCase.execute(new GetOrderCommand(42L, "another_order"))).isInstanceOf(CoreException.class)
            .extracting(exception -> ((CoreException) exception).errorCode()).isEqualTo(ErrorCode.FORBIDDEN);
        verify(orderRepository).findActiveOrderByKey("another_order");
        verifyNoMoreInteractions(orderRepository);
    }

    private static Order order(final Long userId, final String orderKey) {
        final Order order = Order.create(userId, orderKey, OrderAddress.from("06236", "서울 주소", "101호"));
        order.addItem(701L, "상품 701", null, "저자", BigDecimal.TEN, BigDecimal.ONE, 2);
        return order;
    }
}
