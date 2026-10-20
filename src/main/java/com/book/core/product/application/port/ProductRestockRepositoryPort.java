package com.book.core.product.application.port;

import com.book.core.product.domain.ProductInventory;
import com.book.core.product.domain.ProductRestock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ProductRestockRepositoryPort {
    List<Long> findRestockCandidateProductIds();

    Optional<ProductInventory> findInventoryForUpdate(final Long productId);

    boolean existsPending(final Long productId);

    ProductRestock save(final ProductRestock restock);

    List<Long> findPendingIdsCreatedBefore(final LocalDateTime createdBefore);

    Optional<ProductRestock> findPendingForUpdate(final Long restockId);
}
