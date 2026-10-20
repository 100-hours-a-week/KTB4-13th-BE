package com.book.core.coupon.infrastructure.persistence.repository;

import com.book.core.coupon.domain.CouponTarget;
import com.book.core.coupon.domain.enums.CouponTargetType;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface CouponTargetJpaRepository extends JpaRepository<CouponTarget, Long> {
    List<CouponTarget> findByTargetTypeAndTargetIdInAndDeletedAtIsNull(final CouponTargetType targetType, final Collection<Long> targetIds);

    @Query("""
        SELECT DISTINCT targetCategory.id
        FROM ProductCategory productCategory
        JOIN productCategory.category productCategoryNode
        JOIN Category targetCategory
          ON productCategoryNode.path = targetCategory.path
          OR productCategoryNode.path LIKE CONCAT(targetCategory.path, '/%')
        WHERE productCategory.product.id IN :productIds
          AND productCategory.deletedAt IS NULL
          AND productCategoryNode.deletedAt IS NULL
          AND targetCategory.deletedAt IS NULL
        """)
    List<Long> findCategoryIdsByProductIds(final Collection<Long> productIds);
}
