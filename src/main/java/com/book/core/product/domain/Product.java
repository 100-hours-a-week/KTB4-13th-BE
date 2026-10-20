package com.book.core.product.domain;

import com.book.common.domain.BaseEntity;
import com.book.core.book.domain.Book;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
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
    @Version
    @Column(nullable = false)
    private long version;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "book_id", nullable = false, unique = true)
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

    @Getter(AccessLevel.NONE)
    @OneToOne(mappedBy = "product", fetch = FetchType.LAZY, cascade = CascadeType.PERSIST)
    private ProductInventory inventory;

    public Product(final Long id, final Book book, final String name, final String thumbnailUrl, final BigDecimal salePrice,
        final BigDecimal discountedPrice, final BigDecimal costPrice, final Integer stockQuantity) {
        super(id);
        this.book = book;
        this.name = name;
        this.thumbnailUrl = thumbnailUrl;
        this.salePrice = salePrice;
        this.discountedPrice = discountedPrice;
        this.costPrice = costPrice;
        this.inventory = new ProductInventory(this, stockQuantity);
    }

    public Integer stockQuantity() {
        if (inventory == null) {
            return 0;
        }
        return inventory.availableQuantity();
    }
}
