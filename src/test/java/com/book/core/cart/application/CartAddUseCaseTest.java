package com.book.core.cart.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import com.book.core.cart.application.command.AddCartItemCommand;
import com.book.core.cart.application.port.CartItemRepositoryPort;
import com.book.core.cart.application.port.CartRepositoryPort;
import com.book.core.cart.application.usecase.AddCartItemUseCase;
import com.book.core.cart.domain.Cart;
import com.book.core.cart.domain.CartItem;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class CartAddUseCaseTest {
    private final FakeCartRepository cartRepository = new FakeCartRepository();
    private final FakeCartItemRepository cartItemRepository = new FakeCartItemRepository();
    private final AddCartItemUseCase useCase = new AddCartItemUseCase(cartRepository, cartItemRepository);

    @Test
    void 장바구니가_없으면_찾을_수_없다는_오류를_반환한다() {
        assertThatThrownBy(() -> useCase.execute(new AddCartItemCommand(1L, 20L, 2)))
                .isInstanceOfSatisfying(
                        CoreException.class,
                        (final var exception) ->
                                assertThat(exception.errorCode()).isEqualTo(ErrorCode.CART_NOT_FOUND));
    }

    @Test
    void 기존_장바구니에_새_상품을_추가한다() {
        final var cart = new Cart(1L, 1L);
        cartRepository.cart = cart;

        useCase.execute(new AddCartItemCommand(1L, 20L, 2));

        assertThat(cartItemRepository.savedItem).satisfies(item -> {
            assertThat(item.productId()).isEqualTo(20L);
            assertThat(item.quantity()).isEqualTo(2);
        });
    }

    @Test
    void 기존_상품의_수량을_대체하고_삭제된_상품은_활성화한다() {
        final var cart = new Cart(1L, 1L);
        final var item = CartItem.from(1L, 20L, 2);
        item.delete();
        cartRepository.cart = cart;
        cartItemRepository.item = Optional.of(item);

        useCase.execute(new AddCartItemCommand(1L, 20L, 4));

        assertThat(item.quantity()).isEqualTo(4);
        assertThat(item.isActive()).isTrue();
    }

    @Test
    void 새_상품의_잘못된_수량은_상품을_추가하지_않는다() {
        final var cart = new Cart(1L, 1L);
        cartRepository.cart = cart;

        assertThatThrownBy(() -> useCase.execute(new AddCartItemCommand(1L, 20L, 501)))
                .isInstanceOfSatisfying(
                        CoreException.class,
                        (final var exception) ->
                                assertThat(exception.errorCode()).isEqualTo(ErrorCode.INVALID_CART_ITEM_QUANTITY));

        assertThat(cartItemRepository.savedItem).isNull();
    }

    @Test
    void 활성_상품이_30개면_새_상품을_추가하지_않는다() {
        final var cart = new Cart(1L, 1L);
        cartRepository.cart = cart;
        cartItemRepository.activeItemCount = 30;

        assertThatThrownBy(() -> useCase.execute(new AddCartItemCommand(1L, 21L, 1)))
                .isInstanceOfSatisfying(
                        CoreException.class,
                        (final var exception) ->
                                assertThat(exception.errorCode()).isEqualTo(ErrorCode.CART_ITEM_LIMIT_EXCEEDED));

        assertThat(cartItemRepository.savedItem).isNull();
    }

    @Test
    void 활성_상품이_30개면_삭제된_상품을_복구하지_않는다() {
        final var cart = new Cart(1L, 1L);
        final var item = CartItem.from(1L, 20L, 2);
        item.delete();
        cartRepository.cart = cart;
        cartItemRepository.item = Optional.of(item);
        cartItemRepository.activeItemCount = 30;

        assertThatThrownBy(() -> useCase.execute(new AddCartItemCommand(1L, 20L, 4)))
                .isInstanceOfSatisfying(
                        CoreException.class,
                        (final var exception) ->
                                assertThat(exception.errorCode()).isEqualTo(ErrorCode.CART_ITEM_LIMIT_EXCEEDED));

        assertThat(item.isDeleted()).isTrue();
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
        private Optional<CartItem> item = Optional.empty();
        private CartItem savedItem;
        private int activeItemCount;

        @Override
        public Optional<CartItem> findActiveByUserIdAndId(final Long userId, final Long cartItemId) {
            return Optional.empty();
        }

        @Override
        public Optional<CartItem> findByCartIdAndProductId(final Long cartId, final Long productId) {
            return item;
        }

        @Override
        public CartItem save(final CartItem cartItem) {
            savedItem = cartItem;
            return cartItem;
        }

        @Override
        public int countActiveByCartId(final Long cartId) {
            return activeItemCount;
        }

        @Override
        public List<CartItem> findActiveByCartId(final Long cartId) {
            return List.of();
        }
    }
}
