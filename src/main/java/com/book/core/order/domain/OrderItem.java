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

    @Column(name = "unit_price", nullable = false, precision = 19, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "total_price", nullable = false, precision = 19, scale = 2)
    private BigDecimal totalPrice;

    @Column(nullable = false)
    private Integer quantity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private OrderItemStatus status;

    private OrderItem(
            final Long id,
            final Order order,
            final Long productId,
            final BigDecimal unitPrice,
            final Integer quantity,
            final OrderItemStatus status) {
        if (order == null
                || productId == null
                || productId <= 0
                || unitPrice == null
                || unitPrice.signum() < 0
                || quantity == null
                || !isQuantityInRange(quantity)
                || status == null) {
            throw new CoreException(ErrorCode.INVALID_REQUEST);
        }
        this.id = id;
        this.order = order;
        this.productId = productId;
        this.unitPrice = unitPrice;
        this.quantity = quantity;
        this.totalPrice = unitPrice.multiply(BigDecimal.valueOf(quantity));
        this.status = status;
    }

    public static boolean isQuantityInRange(final int quantity) {
        return quantity >= MIN_QUANTITY && quantity <= MAX_QUANTITY;
    }

    static OrderItem created(final Order order, final Long productId, final BigDecimal unitPrice, final int quantity) {
        return new OrderItem(null, order, productId, unitPrice, quantity, OrderItemStatus.CREATED);
    }
}
