package com.book.core.order.application.command;

import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import com.book.core.order.domain.OrderStatus;
import java.nio.charset.StandardCharsets;
import java.time.DateTimeException;
import java.time.LocalDateTime;
import java.util.Base64;

// @formatter:off
public record OrderListCursor(LocalDateTime createdAt, Long orderId) {
    public OrderListCursor {
        if (createdAt == null || orderId == null || orderId <= 0) {
            throw new CoreException(ErrorCode.INVALID_REQUEST);
        }
    }

    public static OrderListCursor parse(
            final String token, final OrderStatus status, final LocalDateTime from, final LocalDateTime to) {
        if (token == null) {
            return null;
        }
        if (from == null || to == null) {
            throw new CoreException(ErrorCode.INVALID_REQUEST);
        }

        final String[] parts;
        try {
            final String decoded = new String(Base64.getUrlDecoder().decode(token), StandardCharsets.UTF_8);
            parts = decoded.split("\\|", -1);
        } catch (final IllegalArgumentException exception) {
            throw new CoreException(ErrorCode.INVALID_REQUEST);
        }

        final String expectedStatus = status == null ? "" : status.name();
        if (parts.length != 5 || !parts[2].equals(expectedStatus) || !parts[3].equals(from.toString())
                || !parts[4].equals(to.toString())) {
            throw new CoreException(ErrorCode.INVALID_REQUEST);
        }

        try {
            return new OrderListCursor(LocalDateTime.parse(parts[0]), Long.parseLong(parts[1]));
        } catch (final DateTimeException | NumberFormatException exception) {
            throw new CoreException(ErrorCode.INVALID_REQUEST);
        }
    }

    public String toToken(final OrderStatus status, final LocalDateTime from, final LocalDateTime to) {
        final String statusValue = status == null ? "" : status.name();
        final String value = String.join("|", createdAt.toString(), orderId.toString(), statusValue, from.toString(), to.toString());
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }
}
// @formatter:on
