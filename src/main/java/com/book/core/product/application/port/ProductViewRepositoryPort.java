package com.book.core.product.application.port;

import com.book.core.product.domain.ProductView;
import java.time.LocalDateTime;
import java.util.List;

public interface ProductViewRepositoryPort {
    ProductView save(final ProductView productView);

    List<Long> findLowStockNotificationCandidateUserIds(final Long productId, final LocalDateTime viewedFrom,
        final LocalDateTime referenceTime);
}
