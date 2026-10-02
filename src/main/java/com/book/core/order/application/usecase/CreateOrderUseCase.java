package com.book.core.order.application.usecase;

import com.book.common.annotation.UseCase;
import com.book.core.order.application.port.OrderRepositoryPort;
import com.book.core.order.application.result.CreateOrderItemResult;
import com.book.core.order.application.result.CreateOrderResult;
import com.book.core.order.domain.Order;
import com.book.core.order.domain.OrderItem;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
public class CreateOrderUseCase {
    private final OrderRepositoryPort orderRepository;

    @Transactional
    public CreateOrderResult execute(final Order order) {
        final Order savedOrder = orderRepository.save(order);
        final List<CreateOrderItemResult> items =
            savedOrder.items().stream().map((final OrderItem item) -> new CreateOrderItemResult(item.productId(), item.itemName(),
                item.thumbnailUrl(), item.author(), item.salePrice(), item.unitPrice(), item.quantity(), item.totalPrice())).toList();
        return new CreateOrderResult(savedOrder.key(), savedOrder.address() != null, savedOrder.totalPrice(), items);
    }
}
