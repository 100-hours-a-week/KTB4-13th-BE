package com.book.core.cart.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.book.core.cart.application.command.GetCartCommand;
import com.book.core.cart.application.port.CartItemRepositoryPort;
import com.book.core.cart.application.port.CartRepositoryPort;
import com.book.core.cart.application.result.GetCartItemResult;
import com.book.core.cart.application.result.GetCartResult;
import com.book.core.cart.application.usecase.GetCartUseCase;
import com.book.core.cart.domain.Cart;
import com.book.core.cart.domain.CartItem;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class GetCartUseCaseTest {
    private final FakeCartRepository cartRepository = new FakeCartRepository();
    private final FakeCartItemRepository cartItemRepository = new FakeCartItemRepository();
    private final GetCartUseCase useCase = new GetCartUseCase(cartRepository, cartItemRepository);

    @Test
    void 장바구니가_없으면_빈_items를_반환한다() {
        final GetCartResult result = useCase.execute(new GetCartCommand(42L));

        assertThat(result.items()).isEmpty();
        assertThat(cartItemRepository.requestedCartId).isNull();
    }

    @Test
    void 활성_장바구니_항목을_저장소가_정렬한_순서와_필드로_반환한다() {
        cartRepository.cart = new Cart(7L, 42L);
        cartItemRepository.items = List.of(new CartItem(11L, 7L, 200L, 2), new CartItem(10L, 7L, 100L, 1));

        final GetCartResult result = useCase.execute(new GetCartCommand(42L));

        assertThat(cartItemRepository.requestedCartId).isEqualTo(7L);
        assertThat(result.items())
                .extracting(GetCartItemResult::cartItemId, GetCartItemResult::productId, GetCartItemResult::quantity)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple(11L, 200L, 2),
                        org.assertj.core.groups.Tuple.tuple(10L, 100L, 1));
    }

    private static final class FakeCartRepository implements CartRepositoryPort {
        private Cart cart;

        @Override
        public Optional<Cart> findByUserIdWithLock(final Long userId) {
            return Optional.ofNullable(cart);
        }

        @Override
        public Optional<Cart> findByUserId(final Long userId) {
            return Optional.ofNullable(cart);
        }
    }

    private static final class FakeCartItemRepository implements CartItemRepositoryPort {
        private List<CartItem> items = List.of();
        private Long requestedCartId;

        @Override
        public Optional<CartItem> findActiveByUserIdAndId(final Long userId, final Long cartItemId) {
            return Optional.empty();
        }

        @Override
        public List<CartItem> findActiveByUserIdAndIds(final Long userId, final List<Long> cartItemIds) {
            return List.of();
        }

        @Override
        public Optional<CartItem> findByCartIdAndProductId(final Long cartId, final Long productId) {
            return Optional.empty();
        }

        @Override
        public CartItem save(final CartItem cartItem) {
            return cartItem;
        }

        @Override
        public int countActiveByCartId(final Long cartId) {
            return 0;
        }

        @Override
        public List<CartItem> findActiveByCartId(final Long cartId) {
            requestedCartId = cartId;
            return items;
        }
    }
}
