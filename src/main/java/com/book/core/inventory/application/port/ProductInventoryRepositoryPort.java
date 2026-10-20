package com.book.core.inventory.application.port;

import com.book.core.inventory.domain.ProductInventory;
import java.util.List;

public interface ProductInventoryRepositoryPort {
    List<ProductInventory> findByProductIds(List<Long> productIds);
}
