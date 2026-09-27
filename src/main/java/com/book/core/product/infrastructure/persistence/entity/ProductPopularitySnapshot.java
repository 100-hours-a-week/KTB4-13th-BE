package com.book.core.product.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

@Entity
@Table(name = "product_popularity_snapshots")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Accessors(fluent = true)
public class ProductPopularitySnapshot {
    @Id
    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "sales_quantity", nullable = false)
    private Long salesQuantity;

    @Column(name = "review_count", nullable = false)
    private Long reviewCount;

    @Column(name = "review_rate", nullable = false, precision = 7, scale = 5)
    private BigDecimal reviewRate;

    @Column(name = "refreshed_at", nullable = false)
    private LocalDateTime refreshedAt;
}
