package com.book.core.cancel.application.service;

import com.book.core.cancel.application.command.CancelCommand;
import com.book.core.cancel.application.usecase.CancelPaymentUseCase;
import com.book.core.cancel.application.usecase.ValidateCancelUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CancelService {
    private final ValidateCancelUseCase validateCancelUseCase;
    private final CancelPaymentUseCase cancelPaymentUseCase;

    public Long cancel(final CancelCommand command) {
        validateCancelUseCase.validate(command.userId(), command.action());

        return cancelPaymentUseCase.execute(command.action());
    }

}
