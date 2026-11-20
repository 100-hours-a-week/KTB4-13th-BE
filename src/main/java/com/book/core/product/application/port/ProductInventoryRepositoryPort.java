package com.book.core.product.application.port;

import com.book.core.product.application.result.ProductInventoryState;
import java.util.Optional;

public interface ProductInventoryRepositoryPort {
    boolean reserve(final Long productId, final int quantity);

    ProductInventoryState state(final Long productId);

    Optional<ProductInventoryState> stateForUpdate(final Long productId);

    boolean release(final Long productId, final int quantity);

    boolean complete(final Long productId, final int quantity);

    boolean restore(final Long productId, final int quantity);

    boolean receive(final Long productId, final int quantity);

}
