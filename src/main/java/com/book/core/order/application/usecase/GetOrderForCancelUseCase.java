package com.book.core.order.application.usecase;

import com.book.common.annotation.UseCase;
import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import com.book.core.order.application.port.OrderRepositoryPort;
import com.book.core.order.domain.Order;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
public class GetOrderForCancelUseCase {
    private final OrderRepositoryPort orderRepository;

    @Transactional(readOnly = true)
    public Order execute(final String orderKey) {
        return orderRepository.findActiveOrderByKey(orderKey).orElseThrow(() -> new CoreException(ErrorCode.NOT_FOUND_DATA));
    }
}
