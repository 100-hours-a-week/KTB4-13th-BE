package com.book.core.payment.application.usecase;

import com.book.common.annotation.UseCase;
import com.book.common.paging.SliceResult;
import com.book.core.payment.application.port.PaymentRepositoryPort;
import com.book.core.payment.domain.Payment;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
public class GetPaymentsForSettlementUseCase {
    private final PaymentRepositoryPort paymentRepository;

    @Transactional(readOnly = true)
    public SliceResult<Payment> execute(final LocalDateTime from, final LocalDateTime to, final int page, final int size) {
        return paymentRepository.findForSettlement(from, to, page, size);
    }
}
