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
    private final DeleteCartItemUseCase deleteCartItemUseCase = new DeleteCartItemUseCase(cartItemRepository);

    @Test
    void 소유한_활성_상품을_삭제하면_soft_delete_상태가_된다() {
        final CartItem cartItem = new CartItem(11L, 1L, 20L, 2);
        cartItemRepository.items = List.of(cartItem);

        deleteCartItemUseCase.execute(new DeleteCartItemCommand(42L, 11L));

        assertThat(cartItem.isDeleted()).isTrue();
    }

    @Test
    void 다른_회원의_상품은_삭제하지_않고_not_found를_던진다() {
        final CartItem cartItem = new CartItem(11L, 1L, 20L, 2);
        cartItemRepository.items = List.of(cartItem);

        assertThatThrownBy(() -> deleteCartItemUseCase.execute(new DeleteCartItemCommand(7L, 11L))).isInstanceOfSatisfying(
            CoreException.class, exception -> assertThat(exception.errorCode()).isEqualTo(ErrorCode.CART_ITEM_NOT_FOUND));

        assertThat(cartItem.isActive()).isTrue();
    }

    @Test
    void 이미_삭제된_상품은_삭제할_수_없다() {
        final CartItem cartItem = new CartItem(11L, 1L, 20L, 2);
        cartItem.delete();
        cartItemRepository.items = List.of(cartItem);

        assertThatThrownBy(() -> deleteCartItemUseCase.execute(new DeleteCartItemCommand(42L, 11L))).isInstanceOfSatisfying(
            CoreException.class, exception -> assertThat(exception.errorCode()).isEqualTo(ErrorCode.CART_ITEM_NOT_FOUND));

        assertThat(cartItem.isDeleted()).isTrue();
    }

    private static final class FakeCartItemRepository implements CartItemRepositoryPort {
        private List<CartItem> items = List.of();

        @Override
        public Optional<CartItem> findActiveByUserIdAndId(final Long userId, final Long cartItemId) {
            if (!userId.equals(42L)) {
                return Optional.empty();
            }
            return items.stream().filter(item -> item.isActive() && item.id().equals(cartItemId)).findFirst();
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
