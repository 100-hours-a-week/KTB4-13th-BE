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

@UseCase
@RequiredArgsConstructor
public class ValidateCancelUseCase {
    private final GetOrderForCancelUseCase getOrderForCancelUseCase;
    private final GetPaymentUseCase getPaymentUseCase;

    @Transactional(readOnly = true)
    public void validate(final Long userId, final CancelAction action) {
        final Order order = getOrderForCancelUseCase.execute(action.orderKey());
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
        if (getPaymentUseCase.execute(order.key()).state() != PaymentState.SUCCESS) {
            throw new CoreException(ErrorCode.PAYMENT_INVALID_STATE);
        }
    }
}
