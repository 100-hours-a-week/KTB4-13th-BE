package com.book.core.review.application.port;

import java.time.LocalDateTime;
import java.util.List;

public interface ReviewOrderRepositoryPort {
    List<Long> findRecentPaidOrderItemIds(final Long userId, final Long productId, final LocalDateTime fromDate);
}
