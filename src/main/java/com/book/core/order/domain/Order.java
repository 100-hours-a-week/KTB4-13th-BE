package com.book.core.order.domain;

import com.book.common.domain.BaseTimeEntity;
import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import jakarta.persistence.CascadeType;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

@Entity
@Table(name = "orders")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Accessors(fluent = true)
public class Order extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "`key`", unique = true, length = 255)
    private String key;

    @Column(name = "total_price", nullable = false, precision = 19, scale = 2)
    private BigDecimal totalPrice;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private OrderStatus status;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @Column(name = "canceled_at")
    private LocalDateTime canceledAt;

    @ManyToOne(fetch = FetchType.LAZY, optional = true, cascade = CascadeType.PERSIST)
    @JoinColumn(name = "address_id", nullable = true)
    private OrderAddress address;

    @Getter(AccessLevel.NONE)
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> items = new ArrayList<>();

    private Order(final Long userId, final String key, final OrderAddress address) {
        if (userId == null || userId <= 0 || key == null || key.isBlank()) {
            throw new CoreException(ErrorCode.INVALID_REQUEST);
        }
        this.userId = userId;
        this.key = key;
        this.address = address;
        this.totalPrice = BigDecimal.ZERO;
        this.status = OrderStatus.CREATED;
    }

    public static Order create(final Long userId, final String key, final OrderAddress address) {
        return new Order(userId, key, address);
    }

    public void addItem(final Long productId, final String itemName, final String thumbnailUrl, final String author,
        final BigDecimal salePrice, final BigDecimal unitPrice, final Integer quantity) {
        final OrderItem orderItem = OrderItem.created(this, productId, itemName, thumbnailUrl, author, salePrice, unitPrice, quantity);
        this.items.add(orderItem);
        this.totalPrice = this.totalPrice.add(orderItem.totalPrice());
    }

    public List<OrderItem> items() {
        return List.copyOf(items);
    }
}
