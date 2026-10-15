package com.book.core.cancel.application.usecase;

import com.book.common.annotation.UseCase;
import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import com.book.core.cancel.domain.CancelAction;
import com.book.core.order.application.usecase.GetOrderForCancelUseCase;
import com.book.core.order.domain.Order;
import com.book.core.order.domain.OrderStatus;
import com.book.core.payment.application.usecase.GetPaymentUseCase;
import com.book.core.payment.domain.enums.PaymentState;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

/** 참고 취소 검증의 소유권, 결제 상태, 주문상품 소속과 잔여 수량 조건입니다. */
@UseCase
@RequiredArgsConstructor
public class ValidateCancelUseCase {
    private final GetOrderForCancelUseCase getOrderForCancelUseCase;
    private final GetPaymentUseCase getPaymentUseCase;

    /** 전체 취소는 소유자의 PAID 주문과 SUCCESS 결제만 허용합니다. */
    @Transactional(readOnly = true)
    public void validate(final Long userId, final CancelAction action) {
        final var order = getOrderForCancelUseCase.execute(action.orderKey());
        if (order.status() != OrderStatus.PAID) {
            throw new CoreException(ErrorCode.NOT_FOUND_DATA);
        }
        validateOwnerAndPayment(userId, order);
    }

    private void validateOwnerAndPayment(final Long userId, final Order order) {
        if (!order.userId().equals(userId)) {
            throw new CoreException(ErrorCode.NOT_FOUND_DATA);
        }
        validatePayment(order);
    }

    private void validatePayment(final Order order) {
        if (getPaymentUseCase.execute(order.id()).state() != PaymentState.SUCCESS) {
            throw new CoreException(ErrorCode.PAYMENT_INVALID_STATE);
        }
    }
}
