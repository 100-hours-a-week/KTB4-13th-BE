package com.book.core.order.application;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import com.book.core.order.application.port.OrderRepositoryPort;
import com.book.core.order.application.usecase.ValidateReviewableOrderItemUseCase;
import com.book.core.order.domain.Order;
import com.book.core.order.domain.OrderItem;
import com.book.core.order.domain.OrderItemStatus;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class ValidateReviewableOrderItemUseCaseTest {
    private final OrderRepositoryPort orderRepository = mock(OrderRepositoryPort.class);
    private final ValidateReviewableOrderItemUseCase useCase = new ValidateReviewableOrderItemUseCase(orderRepository);

    @Test
    void 결제_완료한_주문상품의_주문자만_통과한다() {
        final OrderItem orderItem = orderItem(42L, OrderItemStatus.PAID);
        when(orderRepository.findActiveOrderItem(701L)).thenReturn(Optional.of(orderItem));

        assertThatCode(() -> useCase.execute(42L, 701L)).doesNotThrowAnyException();
    }

    @Test
    void 주문상품이_없으면_404_오류를_던진다() {
        when(orderRepository.findActiveOrderItem(701L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(42L, 701L)).isInstanceOf(CoreException.class)
            .extracting(exception -> ((CoreException) exception).errorCode()).isEqualTo(ErrorCode.ORDER_ITEM_NOT_FOUND);
    }

    @Test
    void 미결제_주문상품은_리뷰_자격_오류를_던진다() {
        final OrderItem orderItem = orderItem(42L, OrderItemStatus.CREATED);
        when(orderRepository.findActiveOrderItem(701L)).thenReturn(Optional.of(orderItem));

        assertThatThrownBy(() -> useCase.execute(42L, 701L)).isInstanceOf(CoreException.class).isInstanceOfSatisfying(CoreException.class,
            exception -> assertThat(exception.errorCode()).isEqualTo(ErrorCode.REVIEW_HAS_NOT_ORDER));
    }

    @Test
    void 다른_사용자의_결제_완료_주문상품은_리뷰_자격_오류를_던진다() {
        final OrderItem orderItem = orderItem(24L, OrderItemStatus.PAID);
        when(orderRepository.findActiveOrderItem(701L)).thenReturn(Optional.of(orderItem));

        assertThatThrownBy(() -> useCase.execute(42L, 701L)).isInstanceOf(CoreException.class).isInstanceOfSatisfying(CoreException.class,
            exception -> assertThat(exception.errorCode()).isEqualTo(ErrorCode.REVIEW_HAS_NOT_ORDER));
    }

    private static OrderItem orderItem(final Long userId, final OrderItemStatus status) {
        final OrderItem orderItem = mock(OrderItem.class);
        final Order order = mock(Order.class);
        when(orderItem.order()).thenReturn(order);
        when(order.userId()).thenReturn(userId);
        when(orderItem.status()).thenReturn(status);
        return orderItem;
    }
}
