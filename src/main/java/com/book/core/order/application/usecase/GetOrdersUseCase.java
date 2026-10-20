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
        final List<Order> fetched = orderRepository.findPaidOrders(command.userId(), command.status(), command.paidAtFrom(),
            command.paidAtBefore(), command.cursor(), command.limit() + 1);
        if (fetched.size() <= command.limit()) {
            return GetOrdersResult.of(toSummaries(fetched), null);
        }
        final List<Order> page = fetched.subList(0, command.limit());
        final Order lastOrder = page.getLast();
        final String nextCursor =
            new OrderListCursor(lastOrder.paidAt(), lastOrder.id()).toToken(command.status(), command.from(), command.to());
        return GetOrdersResult.of(toSummaries(page), nextCursor);
    }

    private static List<OrderSummaryResult> toSummaries(final List<Order> orders) {
        return orders.stream().map(OrderSummaryResult::from).toList();
    }
}
