package com.book.core.review.application.port;

import java.util.List;

public interface ReviewOrderItemRepositoryPort {
    List<Long> findConfirmedOrderItemsForProduct(final Long userId, final Long productId);
}
