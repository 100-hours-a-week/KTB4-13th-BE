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
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class OrderCommandConverter {
    public GetOrdersCommand toGetOrdersCommand(final Long userId, final OrderStatus status, final LocalDateTime from,
        final LocalDateTime to, final String cursor, final Integer limit) {
        final LocalDateTime normalizedTo = to == null ? LocalDate.now().atTime(23, 59, 59, 999_999_999) : to;
        final LocalDateTime normalizedFrom = from == null ? normalizedTo.minusYears(1) : from;
        final OrderListCursor parsedCursor = OrderListCursor.parse(cursor, status, normalizedFrom, normalizedTo);
        return new GetOrdersCommand(userId, status, normalizedFrom, normalizedTo, parsedCursor, limit == null ? 20 : limit);
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
        return new CreateOrderCommand(userId, request.addressId(), items);
    }
}
