package com.book.core.cart.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import com.book.core.cart.application.command.DeleteCartItemCommand;
import com.book.core.cart.application.command.DeleteCartItemsCommand;
import com.book.core.cart.application.port.CartItemRepositoryPort;
import com.book.core.cart.application.usecase.DeleteCartItemUseCase;
import com.book.core.cart.application.usecase.DeleteCartItemsUseCase;
import com.book.core.cart.domain.CartItem;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class CartDeleteUseCaseTest {
    private final FakeCartItemRepository cartItemRepository = new FakeCartItemRepository();
    private final DeleteCartItemUseCase deleteCartItemUseCase = new DeleteCartItemUseCase(cartItemRepository);
    private final DeleteCartItemsUseCase deleteCartItemsUseCase = new DeleteCartItemsUseCase(cartItemRepository);

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

    @Test
    void 다건_삭제는_활성_소유_상품을_모두_soft_delete하고_중복_ID는_한번만_처리한다() {
        final CartItem firstItem = new CartItem(11L, 1L, 20L, 2);
        final CartItem secondItem = new CartItem(12L, 1L, 21L, 3);
        cartItemRepository.items = List.of(firstItem, secondItem);

        deleteCartItemsUseCase.execute(new DeleteCartItemsCommand(42L, List.of(11L, 12L, 11L)));

        assertThat(firstItem.isDeleted()).isTrue();
        assertThat(secondItem.isDeleted()).isTrue();
    }

    @Test
    void 다건_삭제_대상에_없는_ID가_포함되면_어떤_상품도_삭제하지_않는다() {
        final CartItem item = new CartItem(11L, 1L, 20L, 2);
        cartItemRepository.items = List.of(item);

        assertThatThrownBy(() -> deleteCartItemsUseCase.execute(new DeleteCartItemsCommand(42L, List.of(11L, 99L)))).isInstanceOfSatisfying(
            CoreException.class, exception -> assertThat(exception.errorCode()).isEqualTo(ErrorCode.CART_ITEM_NOT_FOUND));

        assertThat(item.isActive()).isTrue();
    }

    @Test
    void 다건_삭제_대상에_다른_회원의_상품이_있으면_어떤_상품도_삭제하지_않는다() {
        final CartItem ownedItem = new CartItem(11L, 1L, 20L, 2);
        final CartItem foreignItem = new CartItem(12L, 2L, 21L, 3);
        cartItemRepository.items = List.of(ownedItem, foreignItem);
        cartItemRepository.otherOwnerIds = Map.of(12L, 7L);

        assertThatThrownBy(() -> deleteCartItemsUseCase.execute(new DeleteCartItemsCommand(42L, List.of(11L, 12L)))).isInstanceOfSatisfying(
            CoreException.class, exception -> assertThat(exception.errorCode()).isEqualTo(ErrorCode.CART_ITEM_NOT_FOUND));

        assertThat(ownedItem.isActive()).isTrue();
        assertThat(foreignItem.isActive()).isTrue();
    }

    @Test
    void 다건_삭제_대상에_이미_삭제된_상품이_있으면_활성_상품을_변경하지_않는다() {
        final CartItem activeItem = new CartItem(11L, 1L, 20L, 2);
        final CartItem deletedItem = new CartItem(12L, 1L, 21L, 3);
        deletedItem.delete();
        cartItemRepository.items = List.of(activeItem, deletedItem);

        assertThatThrownBy(() -> deleteCartItemsUseCase.execute(new DeleteCartItemsCommand(42L, List.of(11L, 12L)))).isInstanceOfSatisfying(
            CoreException.class, exception -> assertThat(exception.errorCode()).isEqualTo(ErrorCode.CART_ITEM_NOT_FOUND));

        assertThat(activeItem.isActive()).isTrue();
        assertThat(deletedItem.isDeleted()).isTrue();
    }

    private static final class FakeCartItemRepository implements CartItemRepositoryPort {
        private List<CartItem> items = List.of();
        private Map<Long, Long> otherOwnerIds = Map.of();

        @Override
        public Optional<CartItem> findActiveByUserIdAndId(final Long userId, final Long cartItemId) {
            if (!userId.equals(42L)) {
                return Optional.empty();
            }
            return items.stream().filter(item -> item.isActive() && item.id().equals(cartItemId) && owns(userId, item)).findFirst();
        }

        @Override
        public List<CartItem> findActiveByUserIdAndIds(final Long userId, final List<Long> cartItemIds) {
            return items.stream().filter(item -> item.isActive() && cartItemIds.contains(item.id()) && owns(userId, item)).toList();
        }

        private boolean owns(final Long userId, final CartItem item) {
            return userId.equals(otherOwnerIds.getOrDefault(item.id(), 42L));
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
