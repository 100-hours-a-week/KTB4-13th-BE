package com.book.core.order.api.converter;

import com.book.core.order.api.request.CreateOrderRequest;
import com.book.core.order.application.command.CreateOrderCommand;
import com.book.core.order.application.command.CreateOrderItemCommand;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class OrderCommandConverter {
    public CreateOrderCommand toCreateOrderCommand(final Long userId, final CreateOrderRequest request) {
        final List<CreateOrderItemCommand> items = request.items().stream()
                .map(item -> new CreateOrderItemCommand(item.itemId(), item.quantity()))
                .toList();
        return new CreateOrderCommand(userId, request.addressId(), items);
    }
}
