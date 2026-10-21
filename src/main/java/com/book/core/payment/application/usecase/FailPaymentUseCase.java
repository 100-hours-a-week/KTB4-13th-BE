package com.book.core.payment.application.usecase;

import com.book.common.annotation.UseCase;
import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import com.book.core.order.application.usecase.GetPaymentOrderUseCase;
import com.book.core.order.domain.Order;
import com.book.core.order.domain.OrderStatus;
import com.book.core.payment.application.port.PaymentHistoryRepositoryPort;
import com.book.core.payment.application.port.PaymentRepositoryPort;
import com.book.core.payment.domain.Payment;
import com.book.core.payment.domain.PaymentHistory;
import com.book.core.payment.domain.enums.TransactionType;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
@Slf4j
public class FailPaymentUseCase {
    private final PaymentRepositoryPort paymentRepository;
    private final GetPaymentOrderUseCase getPaymentOrderUseCase;
    private final PaymentHistoryRepositoryPort paymentHistoryRepository;

    @Transactional
    public void execute(final String orderKey, final String code, final String message) {
        final Order order = getPaymentOrderUseCase.execute(orderKey, OrderStatus.CREATED);
        final Payment payment = paymentRepository.findByOrderId(order.id()).orElseThrow(() -> new CoreException(ErrorCode.NOT_FOUND_DATA));
        log.atDebug().addKeyValue("paymentId", payment.id()).addKeyValue("orderId", order.id())
            .log("[PAYMENT] 결제 실패 이력 기록을 시작합니다. paymentId={}, orderId={}", payment.id(), order.id());
        paymentHistoryRepository.save(new PaymentHistory(TransactionType.PAYMENT_FAIL, payment.userId(), payment.orderId(), payment.id(),
            "", BigDecimal.valueOf(-1), BigDecimal.valueOf(-1), BigDecimal.valueOf(-1), "[" + code + "] " + message, LocalDateTime.now()));
        log.atInfo().addKeyValue("paymentId", payment.id()).addKeyValue("orderId", order.id())
            .log("[PAYMENT] 결제 실패 이력을 기록했습니다. paymentId={}, orderId={}", payment.id(), order.id());
    }
}
