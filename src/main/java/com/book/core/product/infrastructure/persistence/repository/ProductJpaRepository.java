package com.book.core.product.infrastructure.persistence.repository;

import com.book.common.domain.EntityStatus;
import com.book.core.product.domain.Product;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
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

    @Query("""
            select product
            from Product product
            join fetch product.book book
            where product.status = :status
              and book.status = :status
              and (:cursor is null or product.id < :cursor)
            order by product.createdAt desc, product.id desc
            """)
    List<Product> findAllActive(@Param("status") EntityStatus status, @Param("cursor") Long cursor, Pageable pageable);

    @Query("""
            select product
            from ProductCategory productCategory
            join productCategory.product product
            join fetch product.book book
            join productCategory.category category
            where productCategory.status = :status
              and product.status = :status
              and book.status = :status
              and category.status = :status
              and category.id = :categoryId
              and (:cursor is null or product.id < :cursor)
            order by product.createdAt desc, product.id desc
            """)
    List<Product> findAllActiveByCategory(
            @Param("categoryId") Long categoryId,
            @Param("status") EntityStatus status,
            @Param("cursor") Long cursor,
            Pageable pageable);
}
