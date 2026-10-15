package com.book.core.payment.application.usecase;

import com.book.common.annotation.UseCase;
import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import com.book.core.payment.application.port.PaymentRepositoryPort;
import com.book.core.payment.domain.enums.PaymentState;
import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

/** 참고 PaymentValidator의 승인 금액 및 결제 상태 검증입니다. */
@UseCase
@RequiredArgsConstructor
public class ValidatePaymentUseCase {
    private final PaymentRepositoryPort paymentRepository;

    /** READY 결제와 요청 금액의 BigDecimal.equals 일치를 확인합니다. 참고처럼 스케일이 다르면 거절합니다. */
    @Transactional(readOnly = true)
    public void validateForApprove(final Long orderId, final BigDecimal amount) {
        final var payment = paymentRepository.findByOrderId(orderId).orElseThrow(() -> new CoreException(ErrorCode.NOT_FOUND_DATA));
        if (payment.state() != PaymentState.READY) {
            throw new CoreException(ErrorCode.PAYMENT_INVALID_STATE);
        }
        if (!payment.paidAmount().equals(amount)) {
            throw new CoreException(ErrorCode.PAYMENT_AMOUNT_MISMATCH);
        }
    }

}
