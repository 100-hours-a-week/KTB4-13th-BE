package com.book.core.order.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import com.book.core.order.application.command.GetOrderCommand;
import com.book.core.order.application.port.OrderRepositoryPort;
import com.book.core.order.application.result.OrderAddressResult;
import com.book.core.order.application.result.OrderItemResult;
import com.book.core.order.application.usecase.GetOrderUseCase;
import com.book.core.order.domain.Order;
import com.book.core.order.domain.OrderAddress;
import com.book.core.order.domain.OrderItemStatus;
import com.book.core.order.domain.OrderStatus;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class GetOrderUseCaseTest {
    private final OrderRepositoryPort orderRepository = mock(OrderRepositoryPort.class);
    private final GetOrderUseCase useCase = new GetOrderUseCase(orderRepository);

    @Test
    void 회원의_주문_정보와_배송지와_최초_주문상품을_반환한다() {
        final Order order = order(42L, "order_detail");
        order.addItem(702L, "상품 702", "https://img.test/702.jpg", "저자 2", BigDecimal.TEN, new BigDecimal("3.00"), 1);
        order.paid();
        when(orderRepository.findActiveOrderByKey("order_detail")).thenReturn(Optional.of(order));

        final var result = useCase.execute(new GetOrderCommand(42L, "order_detail"));

        assertThat(result.key()).isEqualTo("order_detail");
        assertThat(result.status()).isEqualTo(OrderStatus.PAID);
        assertThat(result.totalPrice()).isEqualByComparingTo("5.00");
        assertThat(result.paidAt()).isEqualTo(order.paidAt());
        assertThat(result.address()).isEqualTo(new OrderAddressResult("06236", "서울 주소", "101호"));
        assertThat(result.items())
            .extracting(OrderItemResult::productId, OrderItemResult::itemName, OrderItemResult::thumbnailUrl, OrderItemResult::author,
                OrderItemResult::unitPrice, OrderItemResult::totalPrice, OrderItemResult::quantity, OrderItemResult::canceledQuantity,
                OrderItemResult::status)
            .containsExactly(tuple(701L, "상품 701", null, "저자", BigDecimal.ONE, new BigDecimal("2"), 2, 0L, OrderItemStatus.PAID),
                tuple(702L, "상품 702", "https://img.test/702.jpg", "저자 2", new BigDecimal("3.00"), new BigDecimal("3.00"), 1, 0L,
                    OrderItemStatus.PAID));
    }

    @Test
    void 취소한_주문상품은_취소_수량과_상태로_반환하고_삭제된_주문상품은_제외한다() {
        final Order order = order(42L, "order_partial");
        order.addItem(702L, "상품 702", null, "저자", BigDecimal.TEN, BigDecimal.ONE, 1);
        order.addItem(703L, "삭제 상품", null, "저자", BigDecimal.TEN, BigDecimal.ONE, 1);
        order.paid();
        order.items().get(0).cancel(1L);
        order.items().get(1).cancel(1L);
        order.items().get(2).delete();
        order.partialCanceled();
        when(orderRepository.findActiveOrderByKey("order_partial")).thenReturn(Optional.of(order));

        final var result = useCase.execute(new GetOrderCommand(42L, "order_partial"));

        assertThat(result.status()).isEqualTo(OrderStatus.PARTIAL_CANCELED);
        assertThat(result.items())
            .extracting(OrderItemResult::productId, OrderItemResult::quantity, OrderItemResult::canceledQuantity, OrderItemResult::status)
            .containsExactly(tuple(701L, 2, 1L, OrderItemStatus.PARTIAL_CANCELED), tuple(702L, 1, 1L, OrderItemStatus.CANCELED));
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
