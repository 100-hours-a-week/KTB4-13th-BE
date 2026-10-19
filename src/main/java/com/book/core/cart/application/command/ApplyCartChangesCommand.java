package com.book.core.cart.application.command;

import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

/** 유효 기한은 선택적 변경 전제조건이며 요청·제안의 생명주기를 소유하지 않습니다. */
public record ApplyCartChangesCommand(Long userId, List<CartItemChange> items, Instant validUntil) {
    public ApplyCartChangesCommand {
        if (userId == null || userId <= 0 || items == null || items.isEmpty() || items.size() > 30
            || items.stream().anyMatch(Objects::isNull)
            || items.stream().map(CartItemChange::productId).distinct().count() != items.size()) {
            throw new CoreException(ErrorCode.INVALID_REQUEST);
        }
        items = List.copyOf(items);
    }
}
