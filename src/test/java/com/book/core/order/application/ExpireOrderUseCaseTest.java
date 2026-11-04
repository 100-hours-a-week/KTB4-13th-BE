package com.book.core.order.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.book.core.order.application.port.OrderRepositoryPort;
import com.book.core.order.application.usecase.ExpireOrderUseCase;
import com.book.core.order.domain.Order;
import com.book.core.order.domain.OrderStatus;
import com.book.core.payment.application.usecase.HasUnconfirmedCheckoutUseCase;
import com.book.core.payment.application.usecase.ReleaseActivePaymentReservationUseCase;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class ExpireOrderUseCaseTest {
    private final OrderRepositoryPort orders = mock(OrderRepositoryPort.class);
    private final ReleaseActivePaymentReservationUseCase reservations = mock(ReleaseActivePaymentReservationUseCase.class);
    private final HasUnconfirmedCheckoutUseCase checkout = mock(HasUnconfirmedCheckoutUseCase.class);
    private final ExpireOrderUseCase useCase = new ExpireOrderUseCase(orders, checkout, reservations);

    @Test
    void 만료가_확정되면_포인트_예약도_해제한다() {
        final LocalDateTime now = LocalDateTime.of(2026, 1, 1, 12, 0);
        final Order order = Order.create(1L, "order-1", null);
        order.reserveUntil(now);
        when(orders.findActiveOrderByKeyForUpdate("order-1")).thenReturn(Optional.of(order));

        assertThat(useCase.execute("order-1", now)).isEqualTo(Map.of());
        assertThat(order.status()).isEqualTo(OrderStatus.EXPIRED);
        verify(reservations).execute("order-1");
    }

    @Test
    void PG_결과가_확인되지_않으면_만료와_예약_해제를_보류한다() {
        final LocalDateTime now = LocalDateTime.of(2026, 1, 1, 12, 0);
        final Order order = Order.create(1L, "order-2", null);
        order.reserveUntil(now);
        when(orders.findActiveOrderByKeyForUpdate("order-2")).thenReturn(Optional.of(order));
        when(orders.hasPendingPgOperation("order-2")).thenReturn(true);

        assertThat(useCase.execute("order-2", now)).isEmpty();
        assertThat(order.status()).isEqualTo(OrderStatus.CREATED);
        verifyNoInteractions(reservations);
    }

    @Test
    void 결제창_결과가_불명확하면_운영확인을_기록하고_예약을_유지한다() {
        final LocalDateTime now = LocalDateTime.of(2026, 1, 1, 12, 0);
        final Order order = Order.create(1L, "order-3", null);
        order.reserveUntil(now);
        when(orders.findActiveOrderByKeyForUpdate("order-3")).thenReturn(Optional.of(order));
        when(checkout.execute(order.id())).thenReturn(true);

        assertThat(useCase.execute("order-3", now)).isEmpty();
        assertThat(order.status()).isEqualTo(OrderStatus.CREATED);
        verify(checkout).requireReview("order-3", order.id());
        verifyNoInteractions(reservations);
    }
}
