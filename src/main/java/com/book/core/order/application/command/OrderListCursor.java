package com.book.core.order.application.command;

import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import com.book.core.order.domain.OrderStatus;
import java.nio.charset.StandardCharsets;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Base64;

// @formatter:off
public record OrderListCursor(LocalDateTime paidAt, Long orderId) {
    public OrderListCursor {
        if (paidAt == null || orderId == null || orderId <= 0) {
            throw new CoreException(ErrorCode.INVALID_REQUEST);
        }
    }

    public static OrderListCursor parse(final String token, final OrderStatus status, final LocalDate from, final LocalDate to) {
        if (token == null) {
            return null;
        }

        final String[] parts;
        try {
            final String decoded = new String(Base64.getUrlDecoder().decode(token), StandardCharsets.UTF_8);
            parts = decoded.split("\\|", -1);
        } catch (final IllegalArgumentException exception) {
            throw new CoreException(ErrorCode.INVALID_REQUEST);
        }

        if (parts.length != 5 || !parts[2].equals(filterValue(status)) || !parts[3].equals(filterValue(from))
                || !parts[4].equals(filterValue(to))) {
            throw new CoreException(ErrorCode.INVALID_REQUEST);
        }

        try {
            return new OrderListCursor(LocalDateTime.parse(parts[0]), Long.parseLong(parts[1]));
        } catch (final DateTimeException | NumberFormatException exception) {
            throw new CoreException(ErrorCode.INVALID_REQUEST);
        }
    }

    public String toToken(final OrderStatus status, final LocalDate from, final LocalDate to) {
        final String value =
                String.join("|", paidAt.toString(), orderId.toString(), filterValue(status), filterValue(from), filterValue(to));
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    private static String filterValue(final Object value) {
        if (value == null) {
            return "";
        }
        return value.toString();
    }
}
// @formatter:on
