package com.book.core.inventory.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import com.book.core.product.domain.Product;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

@Entity
@Table(name = "product_inventory")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Accessors(fluent = true)
public class ProductInventory {
    @Id
    @Column(name = "product_id", nullable = false)
    private Long productId;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", insertable = false, updatable = false,
        foreignKey = @ForeignKey(name = "fk_product_inventory_product"))
    private Product product;

    @Column(name = "on_hand_quantity", nullable = false)
    private int onHandQuantity;

    @Column(name = "reserved_quantity", nullable = false)
    private int reservedQuantity;

    @Column(name = "reorder_point", nullable = false)
    private int reorderPoint;

    @Column(name = "target_stock", nullable = false)
    private int targetStock;

    public ProductInventory(final Long productId, final int onHandQuantity, final int reservedQuantity,
        final int reorderPoint, final int targetStock) {
        if (productId == null || productId <= 0 || onHandQuantity < 0 || reservedQuantity < 0
            || reservedQuantity > onHandQuantity || reorderPoint < 0 || targetStock <= reorderPoint) {
            throw new IllegalArgumentException("유효하지 않은 상품 재고 정보입니다.");
        }
        this.productId = productId;
        this.onHandQuantity = onHandQuantity;
        this.reservedQuantity = reservedQuantity;
        this.reorderPoint = reorderPoint;
        this.targetStock = targetStock;
    }

    public int availableQuantity() {
        return onHandQuantity - reservedQuantity;
    }
}
