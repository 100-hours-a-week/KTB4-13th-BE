package com.book.core.order.application.usecase;

import com.book.common.annotation.UseCase;
import com.book.core.order.application.port.OrderRepositoryPort;
import com.book.core.order.domain.OrderItem;
import java.util.Collection;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
public class GetOrderItemsForSettlementUseCase {
    private final OrderRepositoryPort orderRepository;

    @Transactional(readOnly = true)
    public List<OrderItem> byOrderIds(final Collection<Long> orderIds) {
        return orderRepository.findOrderItemsByOrderIdIn(orderIds);
    }

    @Transactional(readOnly = true)
    public List<OrderItem> byOrderItemIds(final Collection<Long> orderItemIds) {
        return orderRepository.findOrderItemsByIdIn(orderItemIds);
    }
}
