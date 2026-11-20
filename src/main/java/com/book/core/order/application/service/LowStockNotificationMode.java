package com.book.core.order.application.service;

public enum LowStockNotificationMode {
    V2(3),
    V3(5);

    private final int threshold;

    LowStockNotificationMode(final int threshold) {
        this.threshold = threshold;
    }

    public int threshold() {
        return threshold;
    }
}
