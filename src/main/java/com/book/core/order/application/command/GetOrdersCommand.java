package com.book.core.order.application.command;

import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import com.book.core.order.domain.OrderStatus;
import java.time.LocalDate;
import java.time.LocalDateTime;

// @formatter:off
public record GetOrdersCommand(
        Long userId,
        OrderStatus status,
        LocalDate from,
        LocalDate to,
        OrderListCursor cursor,
        Integer limit) {
    private static final LocalDate MIN_DATE = LocalDate.of(1000, 1, 1);
    private static final LocalDate MAX_DATE = LocalDate.of(9999, 12, 30);

    public GetOrdersCommand {
        if (userId == null || userId <= 0 || status == OrderStatus.CREATED || !isSupported(from) || !isSupported(to)
                || (from != null && to != null && from.isAfter(to)) || limit == null || limit <= 0 || limit > 100) {
            throw new CoreException(ErrorCode.INVALID_REQUEST);
        }
    }

    /** 결제 시각 하한(포함)입니다. 시작일이 없으면 {@code null}입니다. */
    public LocalDateTime paidAtFrom() {
        if (from == null) {
            return null;
        }
        return from.atStartOfDay();
    }

    /** 결제 시각 상한(미포함)입니다. 종료일이 없으면 {@code null}입니다. */
    public LocalDateTime paidAtBefore() {
        if (to == null) {
            return null;
        }
        return to.plusDays(1).atStartOfDay();
    }

    private static boolean isSupported(final LocalDate date) {
        return date == null || (!date.isBefore(MIN_DATE) && !date.isAfter(MAX_DATE));
    }
}
// @formatter:on
