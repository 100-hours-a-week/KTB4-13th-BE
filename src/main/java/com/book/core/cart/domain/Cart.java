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
import jakarta.persistence.Transient;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/** 회원 장바구니의 상품 추가 규칙과 상품 수 제한을 관리합니다. */
@Entity
@Table(name = "carts")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Accessors(fluent = true)
public class Cart extends BaseEntity {
    private static final int MAX_ITEM_COUNT = 30;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Transient
    private List<CartItem> items = new ArrayList<>();

    public Cart(final Long id, final long userId, final List<CartItem> items) {
        if (id != null) {
            CartItem.requireId(id);
        }
        CartItem.requireId(userId);
        this.id = id;
        this.userId = userId;
        this.items = new ArrayList<>(items);
    }

    public List<CartItem> items() {
        return List.copyOf(items);
    }

    public void loadItems(final List<CartItem> items) {
        this.items = new ArrayList<>(items);
    }

    public CartItem add(final long productId, final int quantity) {
        CartItem.requireId(productId);
        CartItem.requireQuantity(quantity);
        final var existing = items.stream()
                .filter((final var item) -> item.productId() == productId)
                .findFirst();
        if (existing.isPresent()) {
            final var replaced = existing.get().replaceQuantity(quantity);
            items.set(items.indexOf(existing.get()), replaced);
            return replaced;
        }
        if (items.size() >= MAX_ITEM_COUNT) {
            throw new CoreException(ErrorType.CART_ITEM_LIMIT_EXCEEDED);
        }
        final var item = new CartItem(null, productId, quantity);
        items.add(item);
        return item;
    }
}
