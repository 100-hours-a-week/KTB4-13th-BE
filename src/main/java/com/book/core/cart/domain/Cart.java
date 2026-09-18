package com.book.core.cart.domain;

import com.book.common.domain.BaseEntity;
import com.book.common.exception.BusinessException;
import com.book.core.cart.domain.exception.CartErrorCode;
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

/** 회원 장바구니의 수량과 항목 수 불변식을 관리합니다. */
@Entity
@Table(name = "carts")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Accessors(fluent = true)
public class Cart extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Transient
    private List<CartItem> items = new ArrayList<>();

    public Cart(final Long id, final long userId, final List<CartItem> items) {
        this(id, Long.valueOf(userId), items);
    }

    private Cart(final Long id, final Long userId, final List<CartItem> items) {
        if (id != null) {
            CartItem.requireId(id);
        }
        if (userId != null) {
            CartItem.requireId(userId);
        }
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
            final var updated = existing.get().changeQuantity(quantity);
            items.set(items.indexOf(existing.get()), updated);
            return updated;
        }
        if (items.size() >= 30) {
            throw new BusinessException(CartErrorCode.OPERATION_NOT_ALLOWED);
        }
        final var item = new CartItem(null, productId, quantity);
        items.add(item);
        return item;
    }

    public CartItem item(final long itemId) {
        return items.stream()
                .filter((final var item) -> item.id() != null && item.id() == itemId)
                .findFirst()
                .orElseThrow(() -> new BusinessException(CartErrorCode.FORBIDDEN));
    }

    public CartItem change(final long itemId, final int quantity) {
        final var old = item(itemId);
        final var updated = old.changeQuantity(quantity);
        items.set(items.indexOf(old), updated);
        return updated;
    }
}
