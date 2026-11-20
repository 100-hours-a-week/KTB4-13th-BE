package com.book.core.product.application.result;

public record ProductInventoryState(int availableQuantity, long lowStockNotificationVersion) {
}
