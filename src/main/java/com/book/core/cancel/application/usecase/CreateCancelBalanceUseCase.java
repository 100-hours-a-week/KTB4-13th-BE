package com.book.core.cancel.application.usecase;

import com.book.common.annotation.UseCase;
import com.book.core.cancel.application.port.CancelBalanceRepositoryPort;
import com.book.core.cancel.domain.CancelBalance;
import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
public class CreateCancelBalanceUseCase {
    private final CancelBalanceRepositoryPort cancelBalanceRepository;

    @Transactional
    public void execute(final Long orderId, final BigDecimal paidAmount, final BigDecimal pointAmount, final BigDecimal couponAmount) {
        cancelBalanceRepository
            .save(new CancelBalance(orderId, paidAmount, pointAmount, couponAmount, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO));
    }
}
