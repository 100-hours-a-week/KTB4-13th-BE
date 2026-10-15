package com.book.core.payment.application.usecase;

import com.book.common.annotation.UseCase;
import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import com.book.core.order.application.usecase.GetPaymentOrderUseCase;
import com.book.core.order.domain.OrderStatus;
import com.book.core.payment.application.port.PaymentRepositoryPort;
import com.book.core.payment.application.port.PaymentHistoryRepositoryPort;
import com.book.core.payment.domain.PaymentHistory;
import com.book.core.payment.domain.enums.TransactionType;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.transaction.annotation.Transactional;

/** 실패 이력을 저장하며 주문 및 결제 상태와 혜택을 변경하지 않습니다. */
@UseCase
@RequiredArgsConstructor
@Slf4j
public class FailPaymentUseCase {
    private final PaymentRepositoryPort paymentRepository;
    private final GetPaymentOrderUseCase getPaymentOrderUseCase;
    private final PaymentHistoryRepositoryPort paymentHistoryRepository;

    /** 원본 실패 코드와 메시지를 업무 이력에 보관합니다. 결제 상태와 혜택은 바꾸지 않으며 외부 원문은 로그에 남기지 않습니다. */
    @Transactional
    public void execute(final String orderKey, final String code, final String message) {
        final var order = getPaymentOrderUseCase.execute(orderKey, OrderStatus.CREATED);
        final var payment = paymentRepository.findByOrderId(order.id()).orElseThrow(() -> new CoreException(ErrorCode.NOT_FOUND_DATA));
        log.debug("결제 실패 이력 기록을 시작합니다. paymentId={}, orderId={}, traceId={}", payment.id(), order.id(), MDC.get("traceId"));
        paymentHistoryRepository.save(new PaymentHistory(TransactionType.PAYMENT_FAIL, payment.userId(), payment.orderId(), payment.id(),
            "", BigDecimal.valueOf(-1), BigDecimal.valueOf(-1), BigDecimal.valueOf(-1), "[" + code + "] " + message, LocalDateTime.now()));
        log.info("결제 실패 이력을 기록했습니다. paymentId={}, orderId={}, traceId={}", payment.id(), order.id(), MDC.get("traceId"));
    }
}
