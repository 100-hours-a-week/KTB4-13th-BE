package com.book.core.cancel.application.command;

import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import com.book.core.cancel.domain.PartialCancelAction;
import com.book.core.payment.application.command.PreparePgOperationCommand;
import com.book.core.payment.domain.PgOperationType;

public record PartialCancelCommand(Long userId, PartialCancelAction action, String requestKey) {
    public PartialCancelCommand {
        if (userId == null || userId <= 0 || action == null || action.orderItemId() == null || action.orderItemId() <= 0
            || action.quantity() <= 0 || requestKey == null || !requestKey.matches("[A-Za-z0-9_-]{1,100}")) {
            throw new CoreException(ErrorCode.INVALID_REQUEST);
        }
    }

    /**
     * 부분 취소 입력을 PG 요청 기록 계약으로 변환한다.
     *
     * <p>
     * 입력 지문에는 작업 유형, 주문 키, 주문 상품 ID와 수량을 고정 순서로 포함한다. 같은 요청 키로 입력을 바꾼 재전송은 거절하고, 같은 상품을 같은 수량으로 다시 취소하는
     * 새 요청은 별도 요청 키로 구분하기 위해서다. 이 문자열 형식은 애플리케이션 내부 계약이며 토스페이먼츠가 요구하는 요청 형식은 아니다. 승인 전용 금액은 사용하지 않으므로
     * {@code null}로 전달한다.
     *
     * <p>
     * PG에 전달할 멱등키는 최초 요청 기록을 저장할 때 별도 UUID로 생성하고, 복구 시에도 저장된 값을 재사용한다. 이는 토스페이먼츠의 동일 멱등키 재전송 계약을 따르기 위한
     * 것이다.
     *
     * @return 요청 키와 부분 취소 입력을 함께 검증할 PG 요청 기록 명령
     * @see <a href="https://docs.tosspayments.com/reference/using-api/authorization#멱등키-헤더">토스페이먼츠 멱등키
     *      헤더</a>
     */
    public PreparePgOperationCommand operation() {
        return new PreparePgOperationCommand(action.orderKey(), userId, PgOperationType.PARTIAL_CANCEL, requestKey,
            "PARTIAL_CANCEL:" + action.orderKey() + ":" + action.orderItemId() + ":" + action.quantity(), null, action.orderItemId(),
            action.quantity());
    }
}
