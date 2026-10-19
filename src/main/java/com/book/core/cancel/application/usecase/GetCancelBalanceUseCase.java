package com.book.core.cancel.application.usecase;

import com.book.common.annotation.UseCase;
import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import com.book.core.cancel.application.port.CancelBalanceRepositoryPort;
import com.book.core.cancel.domain.CancelBalance;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
public class GetCancelBalanceUseCase {
    private final CancelBalanceRepositoryPort cancelBalanceRepository;

    @Transactional(readOnly = true)
    public CancelBalance execute(final Long orderId) {
        return cancelBalanceRepository.findByOrderId(orderId).orElseThrow(() -> new CoreException(ErrorCode.NOT_FOUND_DATA));
    }
}
