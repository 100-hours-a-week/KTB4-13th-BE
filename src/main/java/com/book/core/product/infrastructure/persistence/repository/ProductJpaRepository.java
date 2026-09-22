package com.book.core.product.infrastructure.persistence.repository;

import com.book.common.domain.EntityStatus;
import com.book.core.product.domain.Product;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface ProductJpaRepository extends JpaRepository<Product, Long> {
    @Query("""
            select product
            from Product product
            join fetch product.book book
            where product.id = :productId
              and product.status = :status
              and book.status = :status
            """)
    Optional<Product> findActiveById(@Param("productId") Long productId, @Param("status") EntityStatus status);
}
