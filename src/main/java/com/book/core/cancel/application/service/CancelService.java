package com.book.core.cancel.application.service;

import com.book.core.cancel.application.command.CancelCommand;
import com.book.core.cancel.application.command.PartialCancelCommand;
import com.book.core.cancel.application.usecase.CalculatePartialCancelUseCase;
import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import com.book.core.cancel.application.usecase.CancelPgPaymentUseCase;
import com.book.core.payment.application.usecase.PreparePgOperationUseCase;
import com.book.core.payment.application.usecase.RejectPgOperationUseCase;
import com.book.core.cancel.application.usecase.PartialCancelPaymentUseCase;
import com.book.core.cancel.application.usecase.ValidateCancelUseCase;
import com.book.core.cancel.domain.CancelCalculated;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CancelService {
    private final ValidateCancelUseCase validateCancelUseCase;
    private final CancelPgPaymentUseCase cancelPgPaymentUseCase;
    private final PreparePgOperationUseCase preparePgOperationUseCase;
    private final RejectPgOperationUseCase rejectPgOperationUseCase;
    private final CalculatePartialCancelUseCase calculatePartialCancelUseCase;
    private final PartialCancelPaymentUseCase partialCancelPaymentUseCase;

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
        validateCancelUseCase.validatePartial(command.userId(), command.action());
        // TODO(#331): 검증·계산·쓰기가 같은 스냅샷을 사용하도록 후속 설계를 검토한다.
        final CancelCalculated calculated = calculatePartialCancelUseCase.execute(command.action());

        // TODO(#329): 실제 PG 부분 취소와 DB 실패 보상을 구현한다. 현재는 내부 상태만 변경한다.
        return partialCancelPaymentUseCase.execute(command.action(), calculated);
    }
}
