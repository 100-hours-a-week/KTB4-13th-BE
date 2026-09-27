package com.book.core.order.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import com.book.core.order.application.command.CancelOrderCommand;
import com.book.core.order.application.port.OrderRepositoryPort;
import com.book.core.order.application.usecase.CancelOrderUseCase;
import com.book.core.order.domain.Order;
import com.book.core.order.domain.OrderItem;
import com.book.core.order.domain.OrderItemStatus;
import com.book.core.order.domain.OrderStatus;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;

class CancelOrderUseCaseTest {
    private final OrderRepositoryPort orderRepository = mock(OrderRepositoryPort.class);
    private final CancelOrderUseCase useCase = new CancelOrderUseCase(orderRepository);

    @Test
    void 회원의_생성_주문과_모든_주문상품을_취소한다() {
        final Order order = Order.create(42L, "order_to_cancel", null);
        order.addItem(701L, "상품 701", null, "저자", new BigDecimal("10.00"), new BigDecimal("10.00"), 1);
        when(orderRepository.findActiveOrderByKeyForUpdate("order_to_cancel")).thenReturn(Optional.of(order));

        useCase.execute(new CancelOrderCommand(42L, "order_to_cancel"));

        assertThat(order.status()).isEqualTo(OrderStatus.CANCELED);
        assertThat(order.canceledAt()).isNotNull();
        assertThat(order.items()).extracting(OrderItem::status).containsExactly(OrderItemStatus.CANCELED);
    }

    @Test
    void 존재하지_않는_주문은_찾을_수_없음으로_실패한다() {
        when(orderRepository.findActiveOrderByKeyForUpdate("missing_order")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(new CancelOrderCommand(42L, "missing_order"))).isInstanceOf(CoreException.class)
            .extracting(exception -> ((CoreException) exception).errorCode()).isEqualTo(ErrorCode.ORDER_NOT_FOUND);
        verify(orderRepository).findActiveOrderByKeyForUpdate("missing_order");
        verifyNoMoreInteractions(orderRepository);
    }

    @Test
    void 다른_회원의_주문은_취소하지_않는다() {
        final Order order = Order.create(43L, "another_users_order", null);
        when(orderRepository.findActiveOrderByKeyForUpdate("another_users_order")).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> useCase.execute(new CancelOrderCommand(42L, "another_users_order"))).isInstanceOf(CoreException.class)
            .extracting(exception -> ((CoreException) exception).errorCode()).isEqualTo(ErrorCode.FORBIDDEN);
        verify(orderRepository).findActiveOrderByKeyForUpdate("another_users_order");
        verifyNoMoreInteractions(orderRepository);
    }

    @Test
    void 이미_취소된_주문을_다시_요청하면_상태_충돌로_실패한다() {
        final Order order = Order.create(42L, "already_canceled_order", null);
        order.cancel();
        when(orderRepository.findActiveOrderByKeyForUpdate("already_canceled_order")).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> useCase.execute(new CancelOrderCommand(42L, "already_canceled_order"))).isInstanceOf(CoreException.class)
            .extracting(exception -> ((CoreException) exception).errorCode()).isEqualTo(ErrorCode.ORDER_CANNOT_BE_CANCELED);
        verify(orderRepository).findActiveOrderByKeyForUpdate("already_canceled_order");
        verifyNoMoreInteractions(orderRepository);
    }

    @Test
    void 주문_취소는_쓰기_트랜잭션에서_실행한다() throws NoSuchMethodException {
        final var method = CancelOrderUseCase.class.getMethod("execute", CancelOrderCommand.class);

        assertThat(method.getAnnotation(Transactional.class)).isNotNull();
        assertThat(method.getAnnotation(Transactional.class).readOnly()).isFalse();
    }

    @Test
    void 유효하지_않은_회원_ID로_취소_Command를_생성할_수_없다() {
        assertThatThrownBy(() -> new CancelOrderCommand(0L, "order_test")).isInstanceOf(CoreException.class)
            .extracting(exception -> ((CoreException) exception).errorCode()).isEqualTo(ErrorCode.INVALID_REQUEST);
    }
}
