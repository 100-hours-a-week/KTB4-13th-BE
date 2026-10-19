package com.book.core.order.application.result;

import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import com.book.core.cancel.domain.Cancel;
import com.book.core.cancel.domain.enums.CancelType;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

// @formatter:off
public record OrderCancellationResult(
        CancelType type,
        BigDecimal refundedAmount,
        LocalDateTime canceledAt,
        List<OrderCancellationItemResult> items) {
    /** 전체 취소는 주문상품 전체와 최초 수량으로, 부분 취소는 저장된 주문상품과 취소 수량으로 변환합니다. */
    public static OrderCancellationResult of(final Cancel cancel, final List<OrderItemResult> orderItems) {
        final List<OrderCancellationItemResult> items = switch (cancel.type()) {
            case ALL -> orderItems.stream()
                    .map((final OrderItemResult item) -> OrderCancellationItemResult.of(item, item.quantity()))
                    .toList();
            case PARTIAL -> List.of(OrderCancellationItemResult.of(canceledItem(cancel, orderItems), cancel.canceledQuantity()));
        };
        return new OrderCancellationResult(cancel.type(), cancel.canceledPaidAmount(), cancel.canceledAt(), items);
    }

    private static OrderItemResult canceledItem(final Cancel cancel, final List<OrderItemResult> orderItems) {
        if (cancel.canceledQuantity() <= 0) {
            throw new CoreException(ErrorCode.ORDER_DATA_INCONSISTENT);
        }
        return orderItems.stream()
                .filter((final OrderItemResult item) -> item.orderItemId().equals(cancel.orderItemId()))
                .findFirst()
                .orElseThrow(() -> new CoreException(ErrorCode.ORDER_DATA_INCONSISTENT));
    }
}
// @formatter:on
