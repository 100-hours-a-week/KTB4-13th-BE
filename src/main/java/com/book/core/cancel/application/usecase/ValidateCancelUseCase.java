package com.book.core.cancel.application.usecase;

import com.book.core.order.domain.OrderItem;

import com.book.common.annotation.UseCase;
import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import com.book.core.cancel.domain.CancelAction;
import com.book.core.cancel.domain.PartialCancelAction;
import com.book.core.order.application.usecase.GetOrderForCancelUseCase;
import com.book.core.order.application.usecase.GetOrderItemForCancelUseCase;
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
    private final GetOrderItemForCancelUseCase getOrderItemForCancelUseCase;

    @Transactional(readOnly = true)
    public void validate(final Long userId, final CancelAction action) {
        final Order order = getOrderForCancelUseCase.execute(action.orderKey());
        if (order.status() != OrderStatus.PAID) {
            throw new CoreException(ErrorCode.NOT_FOUND_DATA);
        }
        validateOwnerAndPayment(userId, order);
    }

    @Transactional(readOnly = true)
    public void validatePartial(final Long userId, final PartialCancelAction action) {
        final Order order = getOrderForCancelUseCase.execute(action.orderKey());
        if (!order.userId().equals(userId)) {
            throw new CoreException(ErrorCode.NOT_FOUND_DATA);
        }
        if (order.status() != OrderStatus.PAID && order.status() != OrderStatus.PARTIAL_CANCELED) {
            throw new CoreException(ErrorCode.PAYMENT_INVALID_STATE);
        }
        validatePayment(order);
        final OrderItem targetItem = getOrderItemForCancelUseCase.execute(action.orderItemId());
        if (!targetItem.order().id().equals(order.id())) {
            throw new CoreException(ErrorCode.NOT_FOUND_DATA);
        }
        if (targetItem.cancellableQuantity() < action.quantity()) {
            throw new CoreException(ErrorCode.INVALID_REQUEST);
        }
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
