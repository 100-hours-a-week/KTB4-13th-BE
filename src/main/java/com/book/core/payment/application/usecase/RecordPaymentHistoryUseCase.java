package com.book.core.payment.application.usecase;

import com.book.common.annotation.UseCase;
import com.book.core.payment.application.port.PaymentHistoryRepositoryPort;
import com.book.core.payment.domain.PaymentHistory;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
public class RecordPaymentHistoryUseCase {
    private final PaymentHistoryRepositoryPort paymentHistoryRepository;

    @Transactional
    public void execute(final PaymentHistory history) {
        paymentHistoryRepository.save(history);
    }
}
