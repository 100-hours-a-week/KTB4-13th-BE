package com.book.core.payment.application.usecase;

import com.book.core.payment.domain.Payment;

import com.book.common.annotation.UseCase;
import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import com.book.core.payment.application.port.PaymentRepositoryPort;
import com.book.core.payment.domain.enums.PaymentState;
import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
public class ValidatePaymentUseCase {
    private final PaymentRepositoryPort paymentRepository;

    @Transactional(readOnly = true)
    public void validateForApprove(final Long orderId, final BigDecimal amount) {
        final Payment payment = paymentRepository.findByOrderId(orderId).orElseThrow(() -> new CoreException(ErrorCode.NOT_FOUND_DATA));
        if (payment.state() != PaymentState.READY) {
            throw new CoreException(ErrorCode.PAYMENT_INVALID_STATE);
        }
        if (!payment.paidAmount().equals(amount)) {
            throw new CoreException(ErrorCode.PAYMENT_AMOUNT_MISMATCH);
        }
    }

    /** 실패 콜백을 기록할 결제가 존재하는지 확인합니다. */
    @Transactional(readOnly = true)
    public void validateForFail(final Long orderId) {
        paymentRepository.findByOrderId(orderId).orElseThrow(() -> new CoreException(ErrorCode.NOT_FOUND_DATA));
    }
}
