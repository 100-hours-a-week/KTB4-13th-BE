package com.book.core.cart.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import com.book.core.cart.application.command.ModifyCartItemCommand;
import com.book.core.cart.application.port.CartItemRepositoryPort;
import com.book.core.cart.application.usecase.ModifyCartItemUseCase;
import com.book.core.cart.domain.CartItem;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class CartModifyUseCaseTest {
    private final FakeCartItemRepository cartItemRepository = new FakeCartItemRepository();
    private final ModifyCartItemUseCase useCase = new ModifyCartItemUseCase(cartItemRepository);

    @Test
    void 요청한_회원의_활성_장바구니_상품_수량을_변경한다() {
        final var item = new CartItem(11L, 1L, 20L, 2);
        cartItemRepository.item = item;
        cartItemRepository.ownerUserId = 42L;

        useCase.execute(new ModifyCartItemCommand(42L, 11L, 4));

        assertThat(item.quantity()).isEqualTo(4);
    }

    @Test
    void 존재하지_않는_장바구니_상품은_변경하지_않는다() {
        assertThatThrownBy(() -> useCase.execute(new ModifyCartItemCommand(42L, 11L, 4)))
                .isInstanceOfSatisfying(
                        CoreException.class,
                        (final var exception) ->
                                assertThat(exception.errorCode()).isEqualTo(ErrorCode.CART_ITEM_NOT_FOUND));
    }

    @Test
    void 삭제된_장바구니_상품은_변경하지_않는다() {
        final var item = new CartItem(11L, 1L, 20L, 2);
        item.delete();
        cartItemRepository.item = item;
        cartItemRepository.ownerUserId = 42L;

        assertThatThrownBy(() -> useCase.execute(new ModifyCartItemCommand(42L, 11L, 4)))
                .isInstanceOfSatisfying(
                        CoreException.class,
                        (final var exception) ->
                                assertThat(exception.errorCode()).isEqualTo(ErrorCode.CART_ITEM_NOT_FOUND));

        assertThat(item.quantity()).isEqualTo(2);
    }

    @Test
    void 다른_회원의_장바구니_상품은_변경하지_않는다() {
        final var item = new CartItem(11L, 1L, 20L, 2);
        cartItemRepository.item = item;
        cartItemRepository.ownerUserId = 7L;

        assertThatThrownBy(() -> useCase.execute(new ModifyCartItemCommand(42L, 11L, 4)))
                .isInstanceOfSatisfying(
                        CoreException.class,
                        (final var exception) ->
                                assertThat(exception.errorCode()).isEqualTo(ErrorCode.CART_ITEM_NOT_FOUND));

        assertThat(item.quantity()).isEqualTo(2);
    }

    private static final class FakeCartItemRepository implements CartItemRepositoryPort {
        private CartItem item;
        private Long ownerUserId;

        @Override
        public Optional<CartItem> findActiveByUserIdAndId(final Long userId, final Long cartItemId) {
            if (item == null || !item.isActive() || !userId.equals(ownerUserId) || !cartItemId.equals(item.id())) {
                return Optional.empty();
            }
            return Optional.of(item);
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
            return List.of();
        }
    }
}
