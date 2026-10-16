package com.book.core.cancel.application.service;

import com.book.core.cancel.application.command.CancelCommand;
import com.book.core.cancel.application.command.PartialCancelCommand;
import com.book.core.cancel.application.usecase.CalculatePartialCancelUseCase;
import com.book.core.cancel.application.usecase.CancelPaymentUseCase;
import com.book.core.cancel.application.usecase.PartialCancelPaymentUseCase;
import com.book.core.cancel.application.usecase.ValidateCancelUseCase;
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

    /** 부분 취소 가능 수량을 검증하고 환급액을 배분한 뒤 수량, 혜택, 잔액과 이력을 함께 변경합니다. */
    public Long partialCancel(final PartialCancelCommand command) {
        validateCancelUseCase.validatePartial(command.userId(), command.action());
        final var calculated = calculatePartialCancelUseCase.execute(command.action());
        // NOTE: PG 부분 취소 API 호출 => calculated.paidAmount 만큼 PG 취소 요청 | 실패 시 예외 발생
        return partialCancelPaymentUseCase.execute(command.action(), calculated);
    }
}
