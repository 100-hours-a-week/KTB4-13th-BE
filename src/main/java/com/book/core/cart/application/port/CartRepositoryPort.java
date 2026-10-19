package com.book.core.cart.application.port;

import com.book.core.cart.domain.Cart;
import java.util.Optional;

public interface CartRepositoryPort {
    Cart save(final Cart cart);

    Optional<Cart> findByUserIdWithLock(final Long userId);

    Optional<Cart> findByUserId(final Long userId);

    /** 회원의 장바구니 행이 없을 때만 만들고, 동시 생성은 UNIQUE(user_id) 충돌을 무시해 한 행으로 수렴시킵니다. */
    void createIfAbsent(final Long userId);
}
