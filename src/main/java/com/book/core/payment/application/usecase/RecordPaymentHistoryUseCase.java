package com.book.core.payment.application.usecase;

import com.book.common.annotation.UseCase;
import com.book.core.payment.application.port.PaymentHistoryRepositoryPort;
import com.book.core.payment.domain.PaymentHistory;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

/** 다른 업무 흐름의 거래 이력을 같은 트랜잭션에 기록합니다. */
@UseCase
@RequiredArgsConstructor
public class RecordPaymentHistoryUseCase {
    private final PaymentHistoryRepositoryPort paymentHistoryRepository;

    @Transactional
    public void execute(final PaymentHistory history) {
        paymentHistoryRepository.save(history);
    }
}
