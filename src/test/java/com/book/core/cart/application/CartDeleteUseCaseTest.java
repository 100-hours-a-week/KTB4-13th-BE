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
import java.util.Optional;
import org.junit.jupiter.api.Test;

class CartDeleteUseCaseTest {
    private final FakeCartItemRepository cartItemRepository = new FakeCartItemRepository();
    private final DeleteCartItemUseCase useCase = new DeleteCartItemUseCase(cartItemRepository);
    private final DeleteCartItemsUseCase deleteItemsUseCase = new DeleteCartItemsUseCase(cartItemRepository);

    @Test
    void 요청한_회원의_활성_장바구니_상품을_논리_삭제한다() {
        final var item = new CartItem(11L, 1L, 20L, 2);
        cartItemRepository.items = List.of(item);
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
        cartItemRepository.items = List.of(item);
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
        cartItemRepository.items = List.of(item);
        cartItemRepository.ownerUserId = 7L;

        assertThatThrownBy(() -> useCase.execute(new DeleteCartItemCommand(42L, 11L)))
                .isInstanceOfSatisfying(
                        CoreException.class,
                        (final var exception) ->
                                assertThat(exception.errorCode()).isEqualTo(ErrorCode.CART_ITEM_NOT_FOUND));

        assertThat(item.isActive()).isTrue();
    }

    @Test
    void 요청한_회원의_활성_장바구니_상품들을_한번에_논리_삭제한다() {
        final var firstItem = new CartItem(11L, 1L, 20L, 2);
        final var secondItem = new CartItem(12L, 1L, 21L, 3);
        cartItemRepository.items = List.of(firstItem, secondItem);
        cartItemRepository.ownerUserId = 42L;

        deleteItemsUseCase.execute(new DeleteCartItemsCommand(42L, List.of(11L, 12L)));

        assertThat(firstItem.isDeleted()).isTrue();
        assertThat(secondItem.isDeleted()).isTrue();
        assertThat(firstItem.quantity()).isEqualTo(2);
        assertThat(secondItem.quantity()).isEqualTo(3);
    }

    @Test
    void 다건_삭제_대상에_존재하지_않는_상품이_있으면_전체를_삭제하지_않는다() {
        final var item = new CartItem(11L, 1L, 20L, 2);
        cartItemRepository.items = List.of(item);
        cartItemRepository.ownerUserId = 42L;

        assertThatThrownBy(() -> deleteItemsUseCase.execute(new DeleteCartItemsCommand(42L, List.of(11L, 99L))))
                .isInstanceOfSatisfying(
                        CoreException.class,
                        (final var exception) ->
                                assertThat(exception.errorCode()).isEqualTo(ErrorCode.CART_ITEM_NOT_FOUND));

        assertThat(item.isActive()).isTrue();
    }

    @Test
    void 다건_삭제_대상에_다른_회원의_상품이_있으면_전체를_삭제하지_않는다() {
        final var item = new CartItem(11L, 1L, 20L, 2);
        cartItemRepository.items = List.of(item);
        cartItemRepository.ownerUserId = 7L;

        assertThatThrownBy(() -> deleteItemsUseCase.execute(new DeleteCartItemsCommand(42L, List.of(11L))))
                .isInstanceOfSatisfying(
                        CoreException.class,
                        (final var exception) ->
                                assertThat(exception.errorCode()).isEqualTo(ErrorCode.CART_ITEM_NOT_FOUND));

        assertThat(item.isActive()).isTrue();
    }

    @Test
    void 다건_삭제_대상에_이미_삭제된_상품이_있으면_전체를_삭제하지_않는다() {
        final var activeItem = new CartItem(11L, 1L, 20L, 2);
        final var deletedItem = new CartItem(12L, 1L, 21L, 3);
        deletedItem.delete();
        cartItemRepository.items = List.of(activeItem, deletedItem);
        cartItemRepository.ownerUserId = 42L;

        assertThatThrownBy(() -> deleteItemsUseCase.execute(new DeleteCartItemsCommand(42L, List.of(11L, 12L))))
                .isInstanceOfSatisfying(
                        CoreException.class,
                        (final var exception) ->
                                assertThat(exception.errorCode()).isEqualTo(ErrorCode.CART_ITEM_NOT_FOUND));

        assertThat(activeItem.isActive()).isTrue();
        assertThat(deletedItem.isDeleted()).isTrue();
    }

    private static final class FakeCartItemRepository implements CartItemRepositoryPort {
        private List<CartItem> items = List.of();
        private Long ownerUserId;

        @Override
        public Optional<CartItem> findActiveByUserIdAndId(final Long userId, final Long cartItemId) {
            return items.stream()
                    .filter(item -> item.isActive() && userId.equals(ownerUserId) && cartItemId.equals(item.id()))
                    .findFirst();
        }

        @Override
        public List<CartItem> findActiveByUserIdAndIds(final Long userId, final List<Long> cartItemIds) {
            return items.stream()
                    .filter(item -> item.isActive() && userId.equals(ownerUserId) && cartItemIds.contains(item.id()))
                    .toList();
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
