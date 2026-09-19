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
@Table(name = "carts")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Accessors(fluent = true)
public class Cart extends BaseEntity {
    private static final int MAX_ITEM_COUNT = 30;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    public Cart(final Long id, final long userId) {
        super(id);
        this.userId = userId;
    }

    public static Cart of(final long userId) {
        return new Cart(null, userId);
    }

    public void validateCanAddItem(final int itemCount) {
        if (itemCount >= MAX_ITEM_COUNT) {
            throw new CoreException(ErrorCode.CART_ITEM_LIMIT_EXCEEDED);
        }
    }
}
