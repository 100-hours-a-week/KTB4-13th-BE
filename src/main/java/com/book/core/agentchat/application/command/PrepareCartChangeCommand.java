package com.book.core.agentchat.application.command;

import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import com.book.core.agentchat.domain.CartChangeLine;
import com.book.core.cart.application.command.CartItemChange;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/** 인증·grounding·관측 검증을 마친 내부 호출만 사용합니다. */
public record PrepareCartChangeCommand(Long userId, String requestKey, List<CartChangeLine> items) {
    public PrepareCartChangeCommand {
        if (userId == null || userId <= 0 || requestKey == null || requestKey.isBlank() || requestKey.length() > 100 || items == null
            || items.isEmpty() || items.size() > 30 || items.stream().anyMatch(Objects::isNull)) {
            throw new CoreException(ErrorCode.INVALID_REQUEST);
        }
        toCartItems(items);
        items = items.stream().sorted(Comparator.comparing(CartChangeLine::productId)).toList();
        if (items.stream().map(CartChangeLine::productId).distinct().count() != items.size()) {
            throw new CoreException(ErrorCode.INVALID_REQUEST);
        }
    }

    public static List<CartItemChange> toCartItems(final List<CartChangeLine> items) {
        return items.stream().map((final CartChangeLine line) -> new CartItemChange(line.productId(), line.expectedQuantity(),
            line.quantity(), line.expectedUnitPrice())).toList();
    }

    public String intent() {
        return items.stream().map((final CartChangeLine line) -> line.productId() + ":" + line.operation() + ":" + line.quantity())
            .collect(Collectors.joining("|"));
    }
}
