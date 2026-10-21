package com.book.core.product.domain;

import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
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
    public static final int INITIAL_ON_HAND_QUANTITY = 20;
    public static final int DEFAULT_REORDER_POINT = 3;
    public static final int DEFAULT_TARGET_STOCK = 20;
    public static final int LOW_STOCK_QUANTITY = 3;

    @Id
    @Column(name = "product_id")
    private Long productId;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id")
    private Product product;

    @Column(name = "on_hand_quantity", nullable = false)
    private int onHandQuantity;

    @Column(name = "reserved_quantity", nullable = false)
    private int reservedQuantity;

    @Column(name = "reorder_point", nullable = false)
    private int reorderPoint;

    @Column(name = "target_stock", nullable = false)
    private int targetStock;

    ProductInventory(final Product product, final int onHandQuantity) {
        if (product == null || onHandQuantity < 0) {
            throw new CoreException(ErrorCode.INVALID_REQUEST);
        }
        this.product = product;
        this.onHandQuantity = onHandQuantity;
        this.reorderPoint = DEFAULT_REORDER_POINT;
        this.targetStock = DEFAULT_TARGET_STOCK;
    }

    public int availableQuantity() {
        return onHandQuantity - reservedQuantity;
    }

    public boolean needsRestock() {
        return onHandQuantity <= reorderPoint;
    }

    public int restockQuantity() {
        return targetStock - onHandQuantity;
    }

    public static boolean entersLowStock(final int availableBefore, final int availableAfter) {
        return availableBefore > LOW_STOCK_QUANTITY && availableAfter <= LOW_STOCK_QUANTITY;
    }
}
