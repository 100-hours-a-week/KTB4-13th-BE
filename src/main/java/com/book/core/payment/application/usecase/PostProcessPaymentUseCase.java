package com.book.core.payment.application.usecase;

import com.book.core.order.domain.Order;
import com.book.core.payment.domain.Payment;

import com.book.common.annotation.UseCase;
import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import com.book.core.cart.application.usecase.DeleteCartItemsByProductsUseCase;
import com.book.core.order.application.usecase.GetPaymentOrderUseCase;
import com.book.core.order.domain.OrderItem;
import com.book.core.order.domain.OrderStatus;
import com.book.core.payment.application.port.PaymentRepositoryPort;
import com.book.core.payment.domain.enums.PaymentState;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;

@Slf4j
@UseCase
@RequiredArgsConstructor
public class PostProcessPaymentUseCase {
    private final PaymentRepositoryPort paymentRepository;
    private final GetPaymentOrderUseCase getPaymentOrderUseCase;
    private final DeleteCartItemsByProductsUseCase deleteCartItemsByProductsUseCase;

    @Async
    public void execute(final Long paymentId, final String orderKey) {
        log.debug("결제 후처리를 시작합니다. paymentId={}", paymentId);
        try {
            final Payment payment = paymentRepository.findById(paymentId).orElseThrow(() -> new CoreException(ErrorCode.NOT_FOUND_DATA));
            if (payment.state() != PaymentState.SUCCESS) {
                throw new CoreException(ErrorCode.PAYMENT_INVALID_STATE);
            }
            final Order order = getPaymentOrderUseCase.execute(orderKey, OrderStatus.PAID);
            deleteCartItemsByProductsUseCase.execute(payment.userId(), order.items().stream().map(OrderItem::productId).toList());
            log.info("결제 후처리를 마쳤습니다. paymentId={}", paymentId);
        } catch (final Exception exception) {
            log.error("결제 후처리에 실패했습니다. paymentId={}, orderKey={}", paymentId, orderKey, exception);
        }
    }
}
