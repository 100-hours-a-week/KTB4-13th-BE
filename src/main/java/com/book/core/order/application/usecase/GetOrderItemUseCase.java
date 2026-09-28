package com.book.core.order.application.usecase;

import com.book.common.annotation.UseCase;
import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import com.book.core.order.application.port.OrderRepositoryPort;
import com.book.core.order.domain.OrderItem;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
public class GetOrderItemUseCase {
    private final OrderRepositoryPort orderRepository;

    @Transactional(readOnly = true)
    public OrderItem execute(final Long orderItemId) {
        return orderRepository.findActiveOrderItem(orderItemId).orElseThrow(() -> new CoreException(ErrorCode.ORDER_ITEM_NOT_FOUND));
    }
}
