package com.book.core.payment.application.usecase;

import com.book.common.annotation.UseCase;
import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import com.book.core.payment.application.port.PaymentAttemptRepositoryPort;
import com.book.core.payment.domain.PaymentAttempt;
import com.book.core.payment.domain.PaymentAttemptState;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
public class GetSuccessfulPaymentPgOrderIdUseCase {
    private final PaymentAttemptRepositoryPort attempts;

    @Transactional(readOnly = true)
    public String execute(final Long paymentId, final String legacyOrderKey) {
        final Optional<PaymentAttempt> attempt = attempts.findLatestByPaymentId(paymentId);
        if (attempt.isEmpty()) {
            return legacyOrderKey;
        }
        if (attempt.get().state() != PaymentAttemptState.COMPLETED) {
            throw new CoreException(ErrorCode.PG_REVIEW_REQUIRED);
        }
        return attempt.get().pgOrderId();
    }
}
