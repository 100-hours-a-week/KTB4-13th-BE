package com.book.core.cart.domain;

import com.book.common.domain.BaseEntity;
import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

@Entity
@Table(name = "cart_item")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Accessors(fluent = true)
public class CartItem extends BaseEntity {
    private static final int MIN_QUANTITY = 1;
    private static final int MAX_QUANTITY = 500;

    @Column(name = "cart_id", nullable = false)
    private Long cartId;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(nullable = false)
    private Integer quantity;

    public CartItem(final Long id, final Long cartId, final Long productId, final Integer quantity) {
        super(id);
        this.cartId = cartId;
        this.productId = productId;
        applyQuantity(quantity);
    }

    /** 수량이 허용 범위에 속하는지 확인합니다. */
    public static boolean isQuantityInRange(final int quantity) {
        return quantity >= MIN_QUANTITY && quantity <= MAX_QUANTITY;
    }

    public static CartItem from(Long cartId, Long productId, Integer quantity) {
        return new CartItem(null, cartId, productId, quantity);
    }

    public void applyQuantity(final int quantity) {
        if (!isQuantityInRange(quantity)) {
            throw new CoreException(ErrorCode.INVALID_CART_ITEM_QUANTITY);
        }
        this.quantity = quantity;
    }
}
