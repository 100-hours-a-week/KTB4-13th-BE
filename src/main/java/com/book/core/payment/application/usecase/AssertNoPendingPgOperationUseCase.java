package com.book.core.payment.application.usecase;

import com.book.common.annotation.UseCase;
import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import com.book.core.payment.application.port.PgOperationRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
public class AssertNoPendingPgOperationUseCase {
    private final PgOperationRepositoryPort operations;

    @Transactional(propagation = Propagation.MANDATORY)
    public void execute(final String orderKey) {
        if (operations.existsPending(orderKey)) {
            throw new CoreException(ErrorCode.PG_PENDING);
        }
    }
}
