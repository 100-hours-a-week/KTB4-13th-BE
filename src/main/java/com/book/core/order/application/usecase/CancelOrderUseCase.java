package com.book.core.order.application.usecase;

import com.book.common.annotation.UseCase;
import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import com.book.core.order.application.command.CancelOrderCommand;
import com.book.core.order.application.port.OrderRepositoryPort;
import com.book.core.order.domain.Order;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
public class CancelOrderUseCase {
    private final OrderRepositoryPort orderRepository;

    @Transactional
    public void execute(final CancelOrderCommand command) {
        final Order order = orderRepository.findActiveOrderByKeyForUpdate(command.orderKey())
            .orElseThrow(() -> new CoreException(ErrorCode.ORDER_NOT_FOUND));
        if (!order.userId().equals(command.userId())) {
            throw new CoreException(ErrorCode.FORBIDDEN);
        }
        order.cancel();
        orderRepository.save(order);
    }
}
