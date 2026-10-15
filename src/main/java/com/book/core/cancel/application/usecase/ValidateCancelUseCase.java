package com.book.core.cancel.application.usecase;

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

/** 참고 취소 검증의 소유권, 결제 상태, 주문상품 소속과 잔여 수량 조건입니다. */
@UseCase
@RequiredArgsConstructor
public class ValidateCancelUseCase {
    private final GetOrderForCancelUseCase getOrderForCancelUseCase;
    private final GetPaymentUseCase getPaymentUseCase;
    private final GetOrderItemForCancelUseCase getOrderItemForCancelUseCase;

    /** 전체 취소는 소유자의 PAID 주문과 SUCCESS 결제만 허용합니다. */
    @Transactional(readOnly = true)
    public void validate(final Long userId, final CancelAction action) {
        final var order = getOrderForCancelUseCase.execute(action.orderKey());
        if (order.status() != OrderStatus.PAID) {
            throw new CoreException(ErrorCode.NOT_FOUND_DATA);
        }
        validateOwnerAndPayment(userId, order);
    }

    /** PAID 또는 PARTIAL_CANCELED 주문의 소유자, SUCCESS 결제, 상품 소속과 잔여 수량을 확인합니다. */
    @Transactional(readOnly = true)
    public void validatePartial(final Long userId, final PartialCancelAction action) {
        final var order = getOrderForCancelUseCase.execute(action.orderKey());
        if (!order.userId().equals(userId)) {
            throw new CoreException(ErrorCode.NOT_FOUND_DATA);
        }
        if (order.status() != OrderStatus.PAID && order.status() != OrderStatus.PARTIAL_CANCELED) {
            throw new CoreException(ErrorCode.PAYMENT_INVALID_STATE);
        }
        validatePayment(order);
        final var targetItem = getOrderItemForCancelUseCase.execute(action.orderItemId());
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
