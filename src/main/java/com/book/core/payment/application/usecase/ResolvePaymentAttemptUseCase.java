package com.book.core.payment.application.usecase;

import com.book.common.annotation.UseCase;
import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import com.book.core.payment.application.port.PaymentAttemptRepositoryPort;
import com.book.core.payment.domain.PaymentAttempt;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
public class ResolvePaymentAttemptUseCase {
    private final PaymentAttemptRepositoryPort attempts;

    @Transactional(readOnly = true)
    public PaymentAttempt execute(final String pgOrderId) {
        return attempts.findByPgOrderId(pgOrderId).orElseThrow(() -> new CoreException(ErrorCode.NOT_FOUND_DATA));
    }
}
