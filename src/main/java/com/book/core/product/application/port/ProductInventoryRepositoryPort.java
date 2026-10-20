package com.book.core.product.application.port;

public interface ProductInventoryRepositoryPort {
    boolean reserve(final Long productId, final int quantity);

    boolean release(final Long productId, final int quantity);

    boolean complete(final Long productId, final int quantity);

    boolean restore(final Long productId, final int quantity);

    int initializeMissing();
}
