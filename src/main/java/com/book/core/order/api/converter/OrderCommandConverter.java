package com.book.core.order.api.converter;

import com.book.core.order.api.request.CreateOrderRequest;
import com.book.core.order.application.command.CancelOrderCommand;
import com.book.core.order.application.command.CreateOrderCommand;
import com.book.core.order.application.command.CreateOrderItemCommand;
import com.book.core.order.application.command.GetOrderCommand;
import com.book.core.order.application.command.GetOrdersCommand;
import com.book.core.order.application.command.OrderListCursor;
import com.book.core.order.domain.OrderStatus;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class OrderCommandConverter {
    private static final int DEFAULT_ORDER_LIST_LIMIT = 20;

    public GetOrdersCommand toGetOrdersCommand(final Long userId, final OrderStatus status, final LocalDate from, final LocalDate to,
        final String cursor, final Integer limit) {
        final OrderListCursor parsedCursor = OrderListCursor.parse(cursor, status, from, to);
        if (limit == null) {
            return new GetOrdersCommand(userId, status, from, to, parsedCursor, DEFAULT_ORDER_LIST_LIMIT);
        }
        return new GetOrdersCommand(userId, status, from, to, parsedCursor, limit);
    }

    public GetOrderCommand toGetOrderCommand(final Long userId, final String orderKey) {
        return new GetOrderCommand(userId, orderKey);
    }

    public CancelOrderCommand toCancelOrderCommand(final Long userId, final String orderKey) {
        return new CancelOrderCommand(userId, orderKey);
    }

    public CreateOrderCommand toCreateOrderCommand(final Long userId, final CreateOrderRequest request) {
        final List<CreateOrderItemCommand> items =
            request.items().stream().map(item -> new CreateOrderItemCommand(item.itemId(), item.quantity())).toList();
        return new CreateOrderCommand(userId, items);
    }
}
