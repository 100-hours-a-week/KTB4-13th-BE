package com.book.core.cart.domain;

import com.book.common.domain.BaseEntity;
import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
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

    @OneToMany(mappedBy = "cart", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CartItem> items = new ArrayList<>();

    public Cart(final Long id, final long userId, final List<CartItem> items) {
        super(id);
        this.userId = userId;
        this.items = new ArrayList<>(items);
    }

    public static Cart of(final long userId) {
        return new Cart(null, userId, new ArrayList<>());
    }

    public List<CartItem> items() {
        return Collections.unmodifiableList(this.items);
    }

    public boolean isFull() {
        return items.size() >= MAX_ITEM_COUNT;
    }

    public CartItem addItem(CartItem cartItem) {
        if (isFull()) {
            throw new CoreException(ErrorCode.CART_ITEM_LIMIT_EXCEEDED);
        }
        items.add(cartItem);
        return cartItem;
    }
}
