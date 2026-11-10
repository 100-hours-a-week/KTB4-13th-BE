package com.book.core.order.application.usecase;

import com.book.common.annotation.UseCase;
import com.book.core.order.application.port.OrderRepositoryPort;
import com.book.core.order.domain.Order;
import com.book.core.order.domain.OrderStatus;
import com.book.core.payment.application.usecase.HasUnconfirmedCheckoutUseCase;
import com.book.core.payment.application.usecase.ReleaseActivePaymentReservationUseCase;
import java.time.LocalDateTime;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
public class ExpireOrderUseCase {
    private final OrderRepositoryPort orderRepository;
    private final HasUnconfirmedCheckoutUseCase hasUnconfirmedCheckout;
    private final ReleaseActivePaymentReservationUseCase releasePaymentReservation;

    @Transactional
    public Map<Long, Integer> execute(final String orderKey, final LocalDateTime now) {
        final Order order = orderRepository.findActiveOrderByKeyForUpdate(orderKey).orElse(null);
        if (order == null || orderRepository.hasPendingPgOperation(order.key()) || order.status() != OrderStatus.CREATED
            || !order.hasReservation() || order.expiresAt().isAfter(now)) {
            return Map.of();
        }
        // NOTE(케빈): 주문 잠금과 포인트 해제의 원자성을 유지하면서 UseCase 간 조율 경계를 어떻게 둘지 검토해 주세요.
        if (hasUnconfirmedCheckout.execute(order.id())) {
            hasUnconfirmedCheckout.requireReview(order.key(), order.id());
            return Map.of();
        }
        if (!order.expire(now)) {
            return Map.of();
        }
        releasePaymentReservation.execute(order.key());
        return order.reservedQuantities();
    }
}
