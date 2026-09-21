package com.book.core.product.domain;

import com.book.common.domain.BaseEntity;
import com.book.core.book.domain.Book;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

@Entity
@Table(name = "products")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Accessors(fluent = true)
public class Product extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "book_id", nullable = false)
    private Book book;

    @Column(nullable = false)
    private String name;

    @Column(name = "thumbnail_url")
    private String thumbnailUrl;

    @Column(name = "sale_price", nullable = false, precision = 19, scale = 2)
    private BigDecimal salePrice;

    @Column(name = "discounted_price", nullable = false, precision = 19, scale = 2)
    private BigDecimal discountedPrice;

    @Column(name = "cost_price", nullable = false, precision = 19, scale = 2)
    private BigDecimal costPrice;

    @Column(name = "stock_quantity", nullable = false)
    private Integer stockQuantity;

    public Product(
            final Long id,
            final Book book,
            final String name,
            final String thumbnailUrl,
            final BigDecimal salePrice,
            final BigDecimal discountedPrice,
            final BigDecimal costPrice,
            final Integer stockQuantity) {
        super(id);
        this.book = book;
        this.name = name;
        this.thumbnailUrl = thumbnailUrl;
        this.salePrice = salePrice;
        this.discountedPrice = discountedPrice;
        this.costPrice = costPrice;
        this.stockQuantity = stockQuantity;
    }
}
