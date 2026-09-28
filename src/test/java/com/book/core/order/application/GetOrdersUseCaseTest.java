package com.book.core.order.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import com.book.core.order.application.command.GetOrdersCommand;
import com.book.core.order.application.command.OrderListCursor;
import com.book.core.order.application.port.OrderRepositoryPort;
import com.book.core.order.application.usecase.GetOrdersUseCase;
import com.book.core.order.domain.Order;
import com.book.core.order.domain.OrderStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;

class GetOrdersUseCaseTest {
    private static final LocalDateTime FROM = LocalDateTime.of(2025, 9, 28, 10, 0);
    private static final LocalDateTime TO = LocalDateTime.of(2026, 9, 28, 10, 0);

    private final OrderRepositoryPort orderRepository = mock(OrderRepositoryPort.class);
    private final GetOrdersUseCase useCase = new GetOrdersUseCase(orderRepository);

    @Test
    void 회원의_필터에_맞는_주문_목록을_반환한다() {
        final GetOrdersCommand command = new GetOrdersCommand(42L, OrderStatus.PAID, FROM, TO, null, 20);
        final Order order = order(2L, "order_new", LocalDateTime.of(2026, 9, 28, 10, 0));
        when(orderRepository.findActiveOrders(42L, OrderStatus.PAID, FROM, TO, null, 21)).thenReturn(List.of(order));

        final var result = useCase.execute(command);

        assertThat(result.orders()).singleElement().satisfies(summary -> {
            assertThat(summary.key()).isEqualTo("order_new");
            assertThat(summary.name()).isEqualTo("상품 701");
        });
        assertThat(result.nextCursor()).isNull();
        verify(orderRepository).findActiveOrders(42L, OrderStatus.PAID, FROM, TO, null, 21);
    }

    @Test
    void limit보다_하나_더_조회해_마지막_노출_주문의_정렬키로_다음_커서를_만든다() {
        final GetOrdersCommand command = new GetOrdersCommand(42L, null, FROM, TO, null, 2);
        final LocalDateTime sameCreatedAt = LocalDateTime.of(2026, 9, 28, 10, 0);
        final List<Order> orders =
            List.of(order(3L, "order_3", sameCreatedAt), order(2L, "order_2", sameCreatedAt), order(1L, "order_1", sameCreatedAt));
        when(orderRepository.findActiveOrders(42L, null, FROM, TO, null, 3)).thenReturn(orders);

        final var result = useCase.execute(command);

        assertThat(result.orders()).extracting(order -> order.key()).containsExactly("order_3", "order_2");
        assertThat(OrderListCursor.parse(result.nextCursor(), null, FROM, TO)).isEqualTo(new OrderListCursor(sameCreatedAt, 2L));
    }

    @Test
    void 날짜_범위가_1년을_넘거나_limit이_범위를_벗어나면_요청_오류다() {
        assertThatThrownBy(() -> new GetOrdersCommand(42L, null, FROM, TO.plusDays(1), null, 20)).isInstanceOf(CoreException.class)
            .extracting(exception -> ((CoreException) exception).errorCode()).isEqualTo(ErrorCode.INVALID_REQUEST);
        assertThatThrownBy(() -> new GetOrdersCommand(42L, null, FROM, TO, null, 101)).isInstanceOf(CoreException.class)
            .extracting(exception -> ((CoreException) exception).errorCode()).isEqualTo(ErrorCode.INVALID_REQUEST);
    }

    @Test
    void 다른_필터로_만든_커서는_재사용할_수_없다() {
        final String cursor = new OrderListCursor(LocalDateTime.of(2026, 9, 28, 10, 0), 2L).toToken(OrderStatus.PAID, FROM, TO);

        assertThatThrownBy(() -> OrderListCursor.parse(cursor, OrderStatus.CREATED, FROM, TO)).isInstanceOf(CoreException.class)
            .extracting(exception -> ((CoreException) exception).errorCode()).isEqualTo(ErrorCode.INVALID_REQUEST);
    }

    private static Order order(final Long id, final String key, final LocalDateTime createdAt) {
        final Order order = mock(Order.class);
        when(order.id()).thenReturn(id);
        when(order.key()).thenReturn(key);
        when(order.name()).thenReturn("상품 701");
        when(order.status()).thenReturn(OrderStatus.PAID);
        when(order.totalPrice()).thenReturn(BigDecimal.TEN);
        when(order.createdAt()).thenReturn(createdAt);
        return order;
    }
}
