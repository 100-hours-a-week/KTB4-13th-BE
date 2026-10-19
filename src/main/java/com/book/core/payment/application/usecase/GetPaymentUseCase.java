package com.book.core.payment.application.usecase;

import com.book.common.annotation.UseCase;
import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import com.book.core.payment.application.port.PaymentRepositoryPort;
import com.book.core.payment.domain.Payment;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
public class GetPaymentUseCase {
    private final PaymentRepositoryPort paymentRepository;

    @Transactional(readOnly = true)
    public Payment execute(final String orderKey) {
        return paymentRepository.findByOrderKey(orderKey).orElseThrow(() -> new CoreException(ErrorCode.NOT_FOUND_DATA));
    }
}
