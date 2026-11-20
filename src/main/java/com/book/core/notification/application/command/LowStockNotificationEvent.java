package com.book.core.notification.application.command;

import java.time.LocalDateTime;

public record LowStockNotificationEvent(Long productId, Long orderUserId, String productName, long lowStockNotificationVersion,
    LocalDateTime referenceTime) {
}
