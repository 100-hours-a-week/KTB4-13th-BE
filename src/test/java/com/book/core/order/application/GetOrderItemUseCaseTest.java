package com.book.core.order.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import com.book.core.order.application.port.OrderRepositoryPort;
import com.book.core.order.application.usecase.GetOrderItemUseCase;
import com.book.core.order.domain.OrderItem;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class GetOrderItemUseCaseTest {
    private final OrderRepositoryPort orderRepository = mock(OrderRepositoryPort.class);
    private final GetOrderItemUseCase useCase = new GetOrderItemUseCase(orderRepository);

    @Test
    void 활성_주문상품을_조회한다() {
        final OrderItem orderItem = mock(OrderItem.class);
        when(orderRepository.findActiveOrderItem(701L)).thenReturn(Optional.of(orderItem));

        assertThat(useCase.execute(701L)).isSameAs(orderItem);
    }

    @Test
    void 활성_주문상품이_없으면_404_오류를_던진다() {
        when(orderRepository.findActiveOrderItem(701L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(701L)).isInstanceOfSatisfying(CoreException.class,
            exception -> assertThat(exception.errorCode()).isEqualTo(ErrorCode.ORDER_ITEM_NOT_FOUND));
    }
}
