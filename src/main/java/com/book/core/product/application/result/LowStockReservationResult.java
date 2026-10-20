package com.book.core.product.application.result;

import java.util.List;

public record LowStockReservationResult(Long productId, int availableQuantity, long lowStockCycle, List<Long> userIds) {
    public LowStockReservationResult {
        userIds = List.copyOf(userIds);
    }
}
