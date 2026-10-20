package com.book.core.product.infrastructure.persistence.repository;

import com.book.core.product.domain.ProductInventory;
import com.book.core.product.domain.ProductRestockStatus;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface ProductInventoryJpaRepository extends JpaRepository<ProductInventory, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select inventory from ProductInventory inventory where inventory.productId = :productId")
    Optional<ProductInventory> findForUpdate(@Param("productId") Long productId);

    @Query("""
            select inventory.productId
            from ProductInventory inventory
            where inventory.onHandQuantity <= inventory.reorderPoint
              and not exists (
                select 1 from ProductRestock restock
                where restock.productId = inventory.productId and restock.status = :pending
              )
            order by inventory.productId
            """)
    List<Long> findRestockCandidateProductIds(@Param("pending") ProductRestockStatus pending);

    @Modifying(flushAutomatically = true)
    @Query("""
            update ProductInventory inventory
            set inventory.reservedQuantity = inventory.reservedQuantity + :quantity
            where inventory.productId = :productId
              and inventory.onHandQuantity - inventory.reservedQuantity >= :quantity
            """)
    int reserve(@Param("productId") Long productId, @Param("quantity") int quantity);

    @Modifying(flushAutomatically = true)
    @Query("""
            update ProductInventory inventory
            set inventory.reservedQuantity = inventory.reservedQuantity - :quantity
            where inventory.productId = :productId
              and inventory.reservedQuantity >= :quantity
            """)
    int release(@Param("productId") Long productId, @Param("quantity") int quantity);

    @Modifying(flushAutomatically = true)
    @Query("""
            update ProductInventory inventory
            set inventory.reservedQuantity = inventory.reservedQuantity - :quantity,
                inventory.onHandQuantity = inventory.onHandQuantity - :quantity
            where inventory.productId = :productId
              and inventory.reservedQuantity >= :quantity
              and inventory.onHandQuantity >= :quantity
            """)
    int complete(@Param("productId") Long productId, @Param("quantity") int quantity);

    @Modifying(flushAutomatically = true)
    @Query("""
            update ProductInventory inventory
            set inventory.onHandQuantity = inventory.onHandQuantity + :quantity
            where inventory.productId = :productId
            """)
    int restore(@Param("productId") Long productId, @Param("quantity") int quantity);

    @Modifying
    @Query(value = """
            INSERT IGNORE INTO product_inventory (product_id, on_hand_quantity, reserved_quantity, reorder_point, target_stock)
            VALUES (:productId, :onHandQuantity, 0, :reorderPoint, :targetStock)
            """, nativeQuery = true)
    int insertIfMissing(@Param("productId") Long productId, @Param("onHandQuantity") int onHandQuantity,
        @Param("reorderPoint") int reorderPoint, @Param("targetStock") int targetStock);
}
