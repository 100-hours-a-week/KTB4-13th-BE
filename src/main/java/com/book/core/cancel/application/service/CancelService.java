package com.book.core.cancel.application.service;

import com.book.core.cancel.application.command.CancelCommand;
import com.book.core.cancel.application.command.PartialCancelCommand;
import com.book.core.cancel.application.usecase.CalculatePartialCancelUseCase;
import com.book.core.cancel.application.usecase.CancelPaymentUseCase;
import com.book.core.cancel.application.usecase.PartialCancelPaymentUseCase;
import com.book.core.cancel.application.usecase.ValidateCancelUseCase;
import com.book.core.cancel.domain.CancelCalculated;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CancelService {
    private final ValidateCancelUseCase validateCancelUseCase;
    private final CancelPaymentUseCase cancelPaymentUseCase;
    private final CalculatePartialCancelUseCase calculatePartialCancelUseCase;
    private final PartialCancelPaymentUseCase partialCancelPaymentUseCase;

    public Long cancel(final CancelCommand command) {
        validateCancelUseCase.validate(command.userId(), command.action());

        return cancelPaymentUseCase.execute(command.action());
    }

    public Long partialCancel(final PartialCancelCommand command) {
        validateCancelUseCase.validatePartial(command.userId(), command.action());
        final CancelCalculated calculated = calculatePartialCancelUseCase.execute(command.action());

        return partialCancelPaymentUseCase.execute(command.action(), calculated);
    }
}
