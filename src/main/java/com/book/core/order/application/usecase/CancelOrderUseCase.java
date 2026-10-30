package com.book.core.order.application.usecase;

import com.book.common.annotation.UseCase;
import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import com.book.core.order.application.command.CancelOrderCommand;
import com.book.core.order.application.port.OrderRepositoryPort;
import com.book.core.order.domain.Order;
import com.book.core.payment.application.usecase.AssertNoPendingPgOperationUseCase;
import com.book.core.payment.application.usecase.HasUnconfirmedCheckoutUseCase;
import com.book.core.payment.application.usecase.ReleaseActivePaymentReservationUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
public class CancelOrderUseCase {
    private final OrderRepositoryPort orderRepository;
    private final AssertNoPendingPgOperationUseCase assertNoPendingPgOperation;
    private final HasUnconfirmedCheckoutUseCase hasUnconfirmedCheckout;
    private final ReleaseActivePaymentReservationUseCase releasePaymentReservation;

    @Transactional
    public void execute(final CancelOrderCommand command) {
        final Order order = orderRepository.findActiveOrderByKeyForUpdate(command.orderKey())
            .orElseThrow(() -> new CoreException(ErrorCode.ORDER_NOT_FOUND));
        if (!order.userId().equals(command.userId())) {
            throw new CoreException(ErrorCode.FORBIDDEN);
        }
        assertNoPendingPgOperation.execute(order.key());
        if (hasUnconfirmedCheckout.execute(order.id())) {
            throw new CoreException(ErrorCode.PG_PENDING);
        }
        order.cancel();
        releasePaymentReservation.execute(order.key());
    }
}
