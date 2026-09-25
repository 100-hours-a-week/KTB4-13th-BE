package com.book.core.cart.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import com.book.core.book.domain.Book;
import com.book.core.cart.application.command.ModifyCartItemCommand;
import com.book.core.cart.application.port.CartItemRepositoryPort;
import com.book.core.cart.application.usecase.ModifyCartItemUseCase;
import com.book.core.cart.domain.CartItem;
import com.book.core.product.application.port.ProductRepositoryPort;
import com.book.core.product.application.usecase.GetProductDetailUseCase;
import com.book.core.product.domain.Product;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class CartModifyUseCaseTest {
    private final FakeCartItemRepository cartItemRepository = new FakeCartItemRepository();
    private final FakeProductRepository productRepository = new FakeProductRepository();
    private final ModifyCartItemUseCase useCase =
            new ModifyCartItemUseCase(cartItemRepository, new GetProductDetailUseCase(productRepository));

    @Test
    void 요청한_회원의_활성_장바구니_상품_수량을_변경한다() {
        final var item = activeItem();
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
                        exception -> assertThat(exception.errorCode()).isEqualTo(ErrorCode.CART_ITEM_NOT_FOUND));
    }

    @Test
    void 삭제된_장바구니_상품은_변경하지_않는다() {
        final var item = activeItem();
        item.delete();
        cartItemRepository.item = item;
        cartItemRepository.ownerUserId = 42L;

        assertThatThrownBy(() -> useCase.execute(new ModifyCartItemCommand(42L, 11L, 4)))
                .isInstanceOfSatisfying(
                        CoreException.class,
                        exception -> assertThat(exception.errorCode()).isEqualTo(ErrorCode.CART_ITEM_NOT_FOUND));

        assertThat(item.quantity()).isEqualTo(2);
    }

    @Test
    void 다른_회원의_장바구니_상품은_변경하지_않는다() {
        final var item = activeItem();
        cartItemRepository.item = item;
        cartItemRepository.ownerUserId = 7L;

        assertThatThrownBy(() -> useCase.execute(new ModifyCartItemCommand(42L, 11L, 4)))
                .isInstanceOfSatisfying(
                        CoreException.class,
                        exception -> assertThat(exception.errorCode()).isEqualTo(ErrorCode.CART_ITEM_NOT_FOUND));

        assertThat(item.quantity()).isEqualTo(2);
    }

    @Test
    void 요청_수량이_재고보다_많으면_기존_수량을_유지한다() {
        final var item = activeItem();
        cartItemRepository.item = item;
        cartItemRepository.ownerUserId = 42L;
        productRepository.product = product(3);

        assertThatThrownBy(() -> useCase.execute(new ModifyCartItemCommand(42L, 11L, 4)))
                .isInstanceOfSatisfying(
                        CoreException.class,
                        exception -> assertThat(exception.errorCode()).isEqualTo(ErrorCode.INSUFFICIENT_PRODUCT_STOCK));

        assertThat(item.quantity()).isEqualTo(2);
    }

    @Test
    void 존재하지_않는_상품이면_기존_수량을_유지한다() {
        final var item = activeItem();
        cartItemRepository.item = item;
        cartItemRepository.ownerUserId = 42L;
        productRepository.product = null;

        assertThatThrownBy(() -> useCase.execute(new ModifyCartItemCommand(42L, 11L, 4)))
                .isInstanceOfSatisfying(
                        CoreException.class,
                        exception -> assertThat(exception.errorCode()).isEqualTo(ErrorCode.PRODUCT_NOT_FOUND));

        assertThat(item.quantity()).isEqualTo(2);
    }

    @Test
    void 삭제된_상품이면_기존_수량을_유지한다() {
        final var item = activeItem();
        cartItemRepository.item = item;
        cartItemRepository.ownerUserId = 42L;
        productRepository.product.delete();

        assertThatThrownBy(() -> useCase.execute(new ModifyCartItemCommand(42L, 11L, 4)))
                .isInstanceOfSatisfying(
                        CoreException.class,
                        exception -> assertThat(exception.errorCode()).isEqualTo(ErrorCode.PRODUCT_NOT_FOUND));

        assertThat(item.quantity()).isEqualTo(2);
    }

    @Test
    void 수량이_범위를_벗어나면_재고_검증_전에_거부한다() {
        final var item = activeItem();
        cartItemRepository.item = item;
        cartItemRepository.ownerUserId = 42L;
        productRepository.product = product(0);

        assertThatThrownBy(() -> useCase.execute(new ModifyCartItemCommand(42L, 11L, 0)))
                .isInstanceOfSatisfying(
                        CoreException.class,
                        exception -> assertThat(exception.errorCode()).isEqualTo(ErrorCode.INVALID_CART_ITEM_QUANTITY));

        assertThat(item.quantity()).isEqualTo(2);
    }

    private static CartItem activeItem() {
        return new CartItem(11L, 1L, 20L, 2);
    }

    private static Product product(final int stockQuantity) {
        final var book = new Book(10L, null, null, "도서명", "작가", null, "출판사", "소설", LocalDate.of(2026, 1, 1), null);
        return new Product(
                20L,
                book,
                "상품명",
                null,
                new BigDecimal("20000.00"),
                new BigDecimal("18000.00"),
                new BigDecimal("12000.00"),
                stockQuantity);
    }

    private static final class FakeProductRepository implements ProductRepositoryPort {
        private Product product = product(500);

        @Override
        public Optional<Product> findActiveById(final Long productId) {
            return Optional.ofNullable(product).filter(Product::isActive);
        }

        @Override
        public List<Product> findActiveProducts(final Long categoryId, final Long cursor, final int limit) {
            return List.of();
        }
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
