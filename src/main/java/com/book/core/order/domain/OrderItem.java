package com.book.core.order.domain;

import com.book.common.domain.BaseTimeEntity;
import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

@Entity
@Table(name = "order_item")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Accessors(fluent = true)
public class OrderItem extends BaseTimeEntity {
    private static final int MIN_QUANTITY = 1;
    private static final int MAX_QUANTITY = 500;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "item_name", length = 255)
    private String itemName;

    @Column(name = "thumbnail_url", length = 255)
    private String thumbnailUrl;

    @Column(length = 255)
    private String author;

    @Column(name = "sale_price", precision = 19, scale = 2)
    private BigDecimal salePrice;

    @Column(name = "unit_price", nullable = false, precision = 19, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "total_price", nullable = false, precision = 19, scale = 2)
    private BigDecimal totalPrice;

    @Column(nullable = false)
    private Integer quantity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private OrderItemStatus status;

    private OrderItem(final Order order, final Long productId, final String itemName, final String thumbnailUrl, final String author,
        final BigDecimal salePrice, final BigDecimal unitPrice, final Integer quantity) {
        if (order == null || productId == null || productId <= 0 || itemName == null || itemName.isBlank() || author == null
            || author.isBlank() || salePrice == null || salePrice.signum() < 0 || unitPrice == null || unitPrice.signum() < 0
            || quantity == null || !isQuantityInRange(quantity)) {
            throw new CoreException(ErrorCode.INVALID_REQUEST);
        }
        this.order = order;
        this.productId = productId;
        this.itemName = itemName;
        this.thumbnailUrl = thumbnailUrl;
        this.author = author;
        this.salePrice = salePrice;
        this.unitPrice = unitPrice;
        this.quantity = quantity;
        this.totalPrice = unitPrice.multiply(BigDecimal.valueOf(quantity));
        this.status = OrderItemStatus.CREATED;
    }

    public static boolean isQuantityInRange(final int quantity) {
        return quantity >= MIN_QUANTITY && quantity <= MAX_QUANTITY;
    }

    static OrderItem created(final Order order, final Long productId, final String itemName, final String thumbnailUrl, final String author,
        final BigDecimal salePrice, final BigDecimal unitPrice, final Integer quantity) {
        return new OrderItem(order, productId, itemName, thumbnailUrl, author, salePrice, unitPrice, quantity);
    }
}
