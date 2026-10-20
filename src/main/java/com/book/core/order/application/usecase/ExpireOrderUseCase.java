package com.book.core.order.application.usecase;

import com.book.common.annotation.UseCase;
import com.book.core.order.application.port.OrderRepositoryPort;
import com.book.core.order.domain.Order;
import java.time.LocalDateTime;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
public class ExpireOrderUseCase {
    private final OrderRepositoryPort orderRepository;

    @Transactional
    public Map<Long, Integer> execute(final String orderKey, final LocalDateTime now) {
        final Order order = orderRepository.findActiveOrderByKeyForUpdate(orderKey).orElse(null);
        if (order == null || orderRepository.hasPayment(order.id()) || !order.expire(now)) {
            return Map.of();
        }
        return order.reservedQuantities();
    }
}
