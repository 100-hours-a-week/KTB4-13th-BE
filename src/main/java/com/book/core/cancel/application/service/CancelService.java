package com.book.core.cancel.application.service;

import com.book.core.cancel.application.command.CancelCommand;
import com.book.core.cancel.application.usecase.CancelPaymentUseCase;
import com.book.core.cancel.application.usecase.ValidateCancelUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** 참고 검증, 계산, PG 호출 지점과 쓰기 처리 순서를 조율합니다. */
@Service
@RequiredArgsConstructor
public class CancelService {
    private final ValidateCancelUseCase validateCancelUseCase;
    private final CancelPaymentUseCase cancelPaymentUseCase;

    /** PAID 주문의 소유자와 결제를 검증한 뒤 전체 취소를 요청하고 취소 ID를 반환합니다. */
    public Long cancel(final CancelCommand command) {
        validateCancelUseCase.validate(command.userId(), command.action());
        // NOTE: PG 취소 API 호출 => 성공 시 다음 로직으로 진행 | 실패 시 예외 발생
        return cancelPaymentUseCase.execute(command.action());
    }

}
