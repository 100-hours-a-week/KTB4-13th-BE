package com.book.core.cart.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import com.book.core.cart.application.command.DeleteCartItemCommand;
import com.book.core.cart.application.port.CartItemRepositoryPort;
import com.book.core.cart.application.usecase.DeleteCartItemUseCase;
import com.book.core.cart.domain.CartItem;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class CartDeleteUseCaseTest {
    private final FakeCartItemRepository cartItemRepository = new FakeCartItemRepository();
    private final DeleteCartItemUseCase useCase = new DeleteCartItemUseCase(cartItemRepository);

    @Test
    void 요청한_회원의_활성_장바구니_상품을_논리_삭제한다() {
        final var item = new CartItem(11L, 1L, 20L, 2);
        cartItemRepository.item = item;
        cartItemRepository.ownerUserId = 42L;

        useCase.execute(new DeleteCartItemCommand(42L, 11L));

        assertThat(item.isDeleted()).isTrue();
        assertThat(item.quantity()).isEqualTo(2);
    }

    @Test
    void 존재하지_않는_장바구니_상품은_삭제하지_않는다() {
        assertThatThrownBy(() -> useCase.execute(new DeleteCartItemCommand(42L, 11L)))
                .isInstanceOfSatisfying(
                        CoreException.class,
                        (final var exception) ->
                                assertThat(exception.errorCode()).isEqualTo(ErrorCode.CART_ITEM_NOT_FOUND));
    }

    @Test
    void 이미_삭제된_장바구니_상품은_상태를_변경하지_않는다() {
        final var item = new CartItem(11L, 1L, 20L, 2);
        item.delete();
        cartItemRepository.item = item;
        cartItemRepository.ownerUserId = 42L;

        assertThatThrownBy(() -> useCase.execute(new DeleteCartItemCommand(42L, 11L)))
                .isInstanceOfSatisfying(
                        CoreException.class,
                        (final var exception) ->
                                assertThat(exception.errorCode()).isEqualTo(ErrorCode.CART_ITEM_NOT_FOUND));

        assertThat(item.isDeleted()).isTrue();
    }

    @Test
    void 다른_회원의_장바구니_상품은_삭제하지_않는다() {
        final var item = new CartItem(11L, 1L, 20L, 2);
        cartItemRepository.item = item;
        cartItemRepository.ownerUserId = 7L;

        assertThatThrownBy(() -> useCase.execute(new DeleteCartItemCommand(42L, 11L)))
                .isInstanceOfSatisfying(
                        CoreException.class,
                        (final var exception) ->
                                assertThat(exception.errorCode()).isEqualTo(ErrorCode.CART_ITEM_NOT_FOUND));

        assertThat(item.isActive()).isTrue();
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
