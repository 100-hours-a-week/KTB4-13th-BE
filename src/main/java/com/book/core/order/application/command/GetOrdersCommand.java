package com.book.core.order.application.command;

import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import com.book.core.order.domain.OrderStatus;
import java.time.LocalDateTime;

// @formatter:off
public record GetOrdersCommand(
        Long userId,
        OrderStatus status,
        LocalDateTime from,
        LocalDateTime to,
        OrderListCursor cursor,
        Integer limit) {
    public GetOrdersCommand {
        if (userId == null || userId <= 0 || from == null || to == null || from.isAfter(to)
                || from.plusYears(1).isBefore(to) || limit == null || limit <= 0 || limit > 100) {
            throw new CoreException(ErrorCode.INVALID_REQUEST);
        }
    }
}
// @formatter:on
