package com.book.core.order.application.usecase;

import com.book.common.annotation.UseCase;
import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import com.book.core.order.application.command.GetOrderCommand;
import com.book.core.order.application.port.OrderRepositoryPort;
import com.book.core.order.application.result.GetOrderResult;
import com.book.core.order.domain.Order;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
public class GetOrderUseCase {
    private final OrderRepositoryPort orderRepository;

    @Transactional(readOnly = true)
    public GetOrderResult execute(final GetOrderCommand command) {
        final Order order =
            orderRepository.findActiveOrderByKey(command.orderKey()).orElseThrow(() -> new CoreException(ErrorCode.ORDER_NOT_FOUND));
        if (!order.userId().equals(command.userId())) {
            throw new CoreException(ErrorCode.FORBIDDEN);
        }
        return GetOrderResult.from(order);
    }
}
