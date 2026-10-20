package com.book.core.product.infrastructure.persistence.repository;

import com.book.core.product.domain.ProductRestock;
import com.book.core.product.domain.ProductRestockStatus;
import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface ProductRestockJpaRepository extends JpaRepository<ProductRestock, Long> {
    boolean existsByProductIdAndStatus(Long productId, ProductRestockStatus status);

    @Query("""
            select restock.id
            from ProductRestock restock
            where restock.status = :status
              and restock.createdAt <= :createdBefore
            order by restock.id
            """)
    List<Long> findIdsByStatusCreatedBefore(@Param("status") ProductRestockStatus status,
        @Param("createdBefore") LocalDateTime createdBefore);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<ProductRestock> findByIdAndStatus(Long id, ProductRestockStatus status);
}
