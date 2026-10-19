package com.book.core.cancel.application.service;

import com.book.core.cancel.application.command.CancelCommand;
import com.book.core.cancel.application.command.PartialCancelCommand;
import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import com.book.core.cancel.application.usecase.CancelPgPaymentUseCase;
import com.book.core.payment.application.usecase.PreparePgOperationUseCase;
import com.book.core.payment.application.usecase.RejectPgOperationUseCase;
import com.book.core.cancel.application.usecase.PartialCancelPgPaymentUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CancelService {
    private final CancelPgPaymentUseCase cancelPgPaymentUseCase;
    private final PreparePgOperationUseCase preparePgOperationUseCase;
    private final RejectPgOperationUseCase rejectPgOperationUseCase;
    private final PartialCancelPgPaymentUseCase partialCancelPgPaymentUseCase;

    public Long cancel(final CancelCommand command) {
        preparePgOperationUseCase.execute(command.operation());
        try {
            return cancelPgPaymentUseCase.execute(command);
        } catch (final CoreException exception) {
            if (exception.errorCode() == ErrorCode.PG_REJECTED) {
                rejectPgOperationUseCase.execute(command.operation());
            }
            throw exception;
        }
    }

    public Long partialCancel(final PartialCancelCommand command) {
        preparePgOperationUseCase.execute(command.operation());
        try {
            return partialCancelPgPaymentUseCase.execute(command);
        } catch (final CoreException exception) {
            if (exception.errorCode() == ErrorCode.PG_REJECTED) {
                rejectPgOperationUseCase.execute(command.operation());
            }
            throw exception;
        }
    }
}
