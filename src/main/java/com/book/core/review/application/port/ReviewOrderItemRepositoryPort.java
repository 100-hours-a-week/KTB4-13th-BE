package com.book.core.review.application.port;

import com.book.core.order.domain.OrderStatus;

import java.time.LocalDateTime;
import java.util.List;

public interface ReviewOrderItemRepositoryPort {
    List<Long> findRecentOrderItemsForProduct(final Long userId, final Long productId, final OrderStatus orderStatus,
        final LocalDateTime fromDate);
}
