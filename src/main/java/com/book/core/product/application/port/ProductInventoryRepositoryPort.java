package com.book.core.product.application.port;

import com.book.core.product.application.result.ProductInventoryReservationResult;

public interface ProductInventoryRepositoryPort {
    ProductInventoryReservationResult reserve(final Long productId, final int quantity);

    boolean release(final Long productId, final int quantity);

    boolean complete(final Long productId, final int quantity);

    boolean restore(final Long productId, final int quantity);

    boolean receive(final Long productId, final int quantity);

}
