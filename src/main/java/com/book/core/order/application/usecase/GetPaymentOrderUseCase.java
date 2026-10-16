package com.book.core.order.application.usecase;

import com.book.common.annotation.UseCase;
import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import com.book.core.order.application.port.OrderRepositoryPort;
import com.book.core.order.domain.Order;
import com.book.core.order.domain.OrderStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
public class GetPaymentOrderUseCase {
    private final OrderRepositoryPort orderRepository;

    @Transactional(readOnly = true)
    public Order execute(final Long userId, final String orderKey) {
        final Order order = execute(orderKey, OrderStatus.CREATED);
        if (!order.userId().equals(userId)) {
            throw new CoreException(ErrorCode.FORBIDDEN);
        }
        if (order.items().isEmpty()) {
            throw new CoreException(ErrorCode.NOT_FOUND_DATA);
        }
        return order;
    }

    @Transactional(readOnly = true)
    public Order execute(final String orderKey, final OrderStatus state) {
        return orderRepository.findActiveOrderByKey(orderKey).filter((final Order order) -> order.status() == state)
            .orElseThrow(() -> new CoreException(ErrorCode.NOT_FOUND_DATA));
    }
}
