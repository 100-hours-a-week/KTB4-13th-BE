package com.book.core.cart.application.command;

import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import com.book.core.cart.domain.CartItem;
import java.math.BigDecimal;

/** quantity가 null이면 기존 상품 제거, 아니면 해당 최종 수량으로 대체합니다. */
public record CartItemChange(Long productId, int expectedQuantity, Integer quantity, BigDecimal expectedUnitPrice) {
    public CartItemChange {
        if (productId == null || productId <= 0 || expectedQuantity < 0) {
            throw new CoreException(ErrorCode.INVALID_REQUEST);
        }
        if (expectedQuantity > 0 && !CartItem.isQuantityInRange(expectedQuantity)) {
            throw new CoreException(ErrorCode.INVALID_REQUEST);
        }
        if (quantity == null) {
            if (expectedQuantity == 0 || expectedUnitPrice != null) {
                throw new CoreException(ErrorCode.INVALID_REQUEST);
            }
        } else {
            CartItem.validateQuantity(quantity);
            if (expectedUnitPrice == null || expectedUnitPrice.signum() < 0) {
                throw new CoreException(ErrorCode.INVALID_REQUEST);
            }
        }
    }

    public boolean isRemoval() {
        return quantity == null;
    }
}
