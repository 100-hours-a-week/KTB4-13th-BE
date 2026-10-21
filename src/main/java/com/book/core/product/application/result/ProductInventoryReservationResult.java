package com.book.core.product.application.result;

public record ProductInventoryReservationResult(boolean reserved, boolean lowStockEntered, int availableQuantity, long lowStockCycle) {
    public static ProductInventoryReservationResult insufficient() {
        return new ProductInventoryReservationResult(false, false, 0, 0);
    }

    public static ProductInventoryReservationResult success() {
        return new ProductInventoryReservationResult(true, false, 0, 0);
    }

    public static ProductInventoryReservationResult lowStock(final int availableQuantity, final long lowStockCycle) {
        return new ProductInventoryReservationResult(true, true, availableQuantity, lowStockCycle);
    }
}
