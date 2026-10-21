package com.book.core.cart.application.port;

import com.book.core.cart.domain.CartItem;
import java.util.List;
import java.util.Optional;

public interface CartItemRepositoryPort {
    /** Cart 버전 검사에 참여한 쓰기 흐름에서 삭제 항목까지 포함한 같은 스냅샷을 읽습니다. */
    List<CartItem> findAllByCartIdForChange(final Long cartId);

    Optional<CartItem> findActiveByUserIdAndId(final Long userId, final Long cartItemId);

    List<CartItem> findActiveByUserIdAndIds(final Long userId, final List<Long> cartItemIds);

    Optional<CartItem> findByCartIdAndProductId(final Long cartId, final Long productId);

    CartItem save(final CartItem cartItem);

    int countActiveByCartId(final Long cartId);

    List<CartItem> findActiveByCartId(final Long cartId);

    List<Long> findActiveUserIdsByProductId(final Long productId);
}
