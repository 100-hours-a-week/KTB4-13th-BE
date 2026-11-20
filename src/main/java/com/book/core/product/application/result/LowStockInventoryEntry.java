package com.book.core.product.application.result;

import java.time.LocalDateTime;

public record LowStockInventoryEntry(ProductInventoryState state, LocalDateTime referenceTime) {
}
