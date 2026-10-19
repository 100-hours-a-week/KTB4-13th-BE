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
public class LockPaymentOrderUseCase {
    private final OrderRepositoryPort orders;

    @Transactional
    public Order execute(final String orderKey) {
        return orders.findActiveOrderByKeyForUpdate(orderKey).orElseThrow(() -> new CoreException(ErrorCode.ORDER_NOT_FOUND));
    }
}
