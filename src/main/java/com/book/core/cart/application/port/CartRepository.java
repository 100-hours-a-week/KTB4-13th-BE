package com.book.core.cart.application.port;

import com.book.core.cart.domain.Cart;
import com.book.core.cart.domain.CartItem;
import java.util.List;
import java.util.Optional;

/** 변경 메서드는 호출한 UseCase의 트랜잭션에 참여합니다. */
public interface CartRepository {
    Optional<Cart> findByUserId(final long userId);
    /** 최초 생성 경쟁까지 직렬화하고 트랜잭션 종료까지 회원 장바구니를 잠급니다. */
    Cart lockOrCreate(final long userId);

    Optional<Cart> lockByUserId(final long userId);

    void saveItem(final long cartId, final CartItem item);

    void deleteItems(final long cartId, final List<Long> itemIds);
}
