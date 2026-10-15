package com.book.core.cancel.api.converter;

import com.book.core.cancel.api.request.CancelRequest;
import com.book.core.cancel.application.command.CancelCommand;
import com.book.core.cancel.domain.CancelAction;
import org.springframework.stereotype.Component;

/** HTTP 값을 참고 취소 액션을 포함한 Command로 변환합니다. */
@Component
public class CancelCommandConverter {
    /** 인증 회원 ID와 주문 키를 전체 취소 Command로 옮깁니다. */
    public CancelCommand toCancelCommand(final Long userId, final CancelRequest request) {
        return new CancelCommand(userId, new CancelAction(request.orderKey()));
    }

}
