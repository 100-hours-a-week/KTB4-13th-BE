package com.book.core.cancel.application.service;

import com.book.core.cancel.application.command.CancelCommand;
import com.book.core.cancel.application.command.PartialCancelCommand;
import com.book.core.cancel.application.usecase.CalculatePartialCancelUseCase;
import com.book.core.cancel.application.usecase.CancelPaymentUseCase;
import com.book.core.cancel.application.usecase.PartialCancelPaymentUseCase;
import com.book.core.cancel.application.usecase.ValidateCancelUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** 참고 검증, 계산, PG 호출 지점과 쓰기 처리 순서를 조율합니다. */
@Service
@RequiredArgsConstructor
public class CancelService {
    private final ValidateCancelUseCase validateCancelUseCase;
    private final CancelPaymentUseCase cancelPaymentUseCase;
    private final CalculatePartialCancelUseCase calculatePartialCancelUseCase;
    private final PartialCancelPaymentUseCase partialCancelPaymentUseCase;

    /** PAID 주문의 소유자와 결제를 검증한 뒤 전체 취소를 요청하고 취소 ID를 반환합니다. */
    public Long cancel(final CancelCommand command) {
        validateCancelUseCase.validate(command.userId(), command.action());
        // NOTE: PG 취소 API 호출 => 성공 시 다음 로직으로 진행 | 실패 시 예외 발생
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
