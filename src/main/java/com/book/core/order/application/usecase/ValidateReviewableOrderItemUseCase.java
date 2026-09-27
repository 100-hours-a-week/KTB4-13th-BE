package com.book.core.order.application.usecase;

import com.book.common.annotation.UseCase;
import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import com.book.core.order.application.port.OrderRepositoryPort;
import com.book.core.order.domain.OrderItem;
import com.book.core.order.domain.OrderItemStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
public class ValidateReviewableOrderItemUseCase {
    private final OrderRepositoryPort orderRepository;

    @Transactional(readOnly = true)
    public void execute(final Long userId, final Long orderItemId) {
        final OrderItem orderItem =
            orderRepository.findActiveOrderItem(orderItemId).orElseThrow(() -> new CoreException(ErrorCode.ORDER_ITEM_NOT_FOUND));
        if (!orderItem.order().userId().equals(userId) || orderItem.status() != OrderItemStatus.PAID) {
            throw new CoreException(ErrorCode.REVIEW_HAS_NOT_ORDER);
        }
    }
}
