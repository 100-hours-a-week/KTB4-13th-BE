package com.book.core.order.application.usecase;

import com.book.common.annotation.UseCase;
import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import com.book.core.cancel.application.port.CancelBalanceRepositoryPort;
import com.book.core.cancel.domain.CancelBalance;
import com.book.core.order.application.command.GetOrderCommand;
import com.book.core.order.application.port.OrderRepositoryPort;
import com.book.core.order.application.result.GetOrderResult;
import com.book.core.order.application.result.OrderPaymentResult;
import com.book.core.order.domain.Order;
import com.book.core.payment.application.port.PaymentRepositoryPort;
import com.book.core.payment.domain.Payment;
import com.book.core.payment.domain.enums.PaymentState;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
public class GetOrderDetailUseCase {
    private final OrderRepositoryPort orderRepository;
    private final PaymentRepositoryPort paymentRepository;
    private final CancelBalanceRepositoryPort cancelBalanceRepository;

    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public GetOrderResult execute(final GetOrderCommand command) {
        final Order order =
            orderRepository.findActiveOrderByKey(command.orderKey()).orElseThrow(() -> new CoreException(ErrorCode.ORDER_NOT_FOUND));
        if (!order.userId().equals(command.userId())) {
            throw new CoreException(ErrorCode.FORBIDDEN);
        }
        final GetOrderResult result = GetOrderResult.from(order);
        if (result.paidAt() == null) {
            return result;
        }
        final Payment payment = paymentRepository.findByOrderKey(order.key())
            .orElseThrow(() -> new CoreException(ErrorCode.ORDER_DATA_INCONSISTENT));
        if (payment.state() != PaymentState.SUCCESS) {
            throw new CoreException(ErrorCode.ORDER_DATA_INCONSISTENT);
        }
        final CancelBalance balance = cancelBalanceRepository.findByOrderId(payment.orderId())
            .orElseThrow(() -> new CoreException(ErrorCode.ORDER_DATA_INCONSISTENT));
        return result.withPayment(OrderPaymentResult.of(payment, balance));
    }
}
