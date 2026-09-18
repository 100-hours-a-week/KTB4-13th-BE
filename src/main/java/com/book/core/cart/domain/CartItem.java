package com.book.core.cart.domain;

import com.book.common.domain.BaseEntity;
import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/** 장바구니 항목의 상품 식별자와 추가 수량 불변식을 관리합니다. */
@Entity
@Table(name = "cart_item")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Accessors(fluent = true)
public class CartItem extends BaseEntity {
    private static final int MIN_QUANTITY = 1;
    private static final int MAX_QUANTITY = 500;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "cart_id", nullable = false)
    private Long cartId;

    @Column(name = "product_id", nullable = false)
    private long productId;

    @Column(nullable = false)
    private int quantity;

    public CartItem(final Long id, final long productId, final int quantity) {
        this(id, null, productId, quantity);
    }

    public CartItem(final long cartId, final long productId, final int quantity) {
        this(null, cartId, productId, quantity);
    }

    private CartItem(final Long id, final Long cartId, final long productId, final int quantity) {
        requireId(productId);
        if (id != null) {
            requireId(id);
        }
        requireQuantity(quantity);
        this.id = id;
        this.cartId = cartId;
        this.productId = productId;
        this.quantity = quantity;
    }

    public static void requireId(final long id) {
        if (id <= 0) {
            throw new CoreException(ErrorType.INVALID_REQUEST);
        }
    }

    public static void requireId(final Long id) {
        if (id == null) {
            throw new CoreException(ErrorType.INVALID_REQUEST);
        }
        requireId(id.longValue());
    }

    public static void requireQuantity(final int quantity) {
        if (quantity < MIN_QUANTITY || quantity > MAX_QUANTITY) {
            throw new CoreException(ErrorType.INVALID_REQUEST);
        }
    }

    public CartItem replaceQuantity(final int quantity) {
        requireQuantity(quantity);
        return new CartItem(id, cartId, productId, quantity);
    }
}
