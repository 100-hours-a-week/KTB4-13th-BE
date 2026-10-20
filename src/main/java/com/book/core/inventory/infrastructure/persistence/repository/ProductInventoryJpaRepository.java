package com.book.core.inventory.infrastructure.persistence.repository;

import com.book.core.inventory.domain.ProductInventory;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

interface ProductInventoryJpaRepository extends JpaRepository<ProductInventory, Long> {
    List<ProductInventory> findByProductIdIn(List<Long> productIds);
}
