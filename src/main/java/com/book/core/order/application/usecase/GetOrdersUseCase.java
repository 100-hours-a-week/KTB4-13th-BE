package com.book.core.order.application.usecase;

import com.book.common.annotation.UseCase;
import com.book.core.order.application.command.GetOrdersCommand;
import com.book.core.order.application.command.OrderListCursor;
import com.book.core.order.application.port.OrderRepositoryPort;
import com.book.core.order.application.result.GetOrdersResult;
import com.book.core.order.application.result.OrderSummaryResult;
import com.book.core.order.domain.Order;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
public class GetOrdersUseCase {
    private final OrderRepositoryPort orderRepository;

    @Transactional(readOnly = true)
    public GetOrdersResult execute(final GetOrdersCommand command) {
        final List<Order> fetched = orderRepository.findActiveOrders(command.userId(), command.status(), command.from(), command.to(),
            command.cursor(), command.limit() + 1);
        final boolean hasNext = fetched.size() > command.limit();
        final List<Order> page = hasNext ? fetched.subList(0, command.limit()) : fetched;
        final String nextCursor;
        if (hasNext) {
            final Order lastOrder = page.getLast();
            nextCursor = new OrderListCursor(lastOrder.createdAt(), lastOrder.id()).toToken(command.status(), command.from(), command.to());
        } else {
            nextCursor = null;
        }
        return GetOrdersResult.of(page.stream().map(OrderSummaryResult::from).toList(), nextCursor);
    }
}
