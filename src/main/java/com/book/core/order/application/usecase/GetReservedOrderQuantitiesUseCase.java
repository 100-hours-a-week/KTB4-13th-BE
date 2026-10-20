package com.book.core.order.application.usecase;

import com.book.common.annotation.UseCase;
import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import com.book.core.order.application.port.OrderRepositoryPort;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
public class GetReservedOrderQuantitiesUseCase {
    private final OrderRepositoryPort orderRepository;

    @Transactional(readOnly = true)
    public Map<Long, Integer> execute(final String orderKey) {
        return orderRepository.findActiveOrderByKey(orderKey).orElseThrow(() -> new CoreException(ErrorCode.ORDER_NOT_FOUND))
            .reservedQuantities();
    }
}
