package com.book.core.cart.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.book.core.cart.application.port.CartItemRepositoryPort;
import com.book.core.cart.application.port.CartRepositoryPort;
import com.book.core.cart.application.result.GetCartItemResult;
import com.book.core.cart.application.result.GetCartResult;
import com.book.core.cart.application.usecase.GetCartUseCase;
import com.book.core.cart.domain.Cart;
import com.book.core.cart.domain.CartItem;
import com.book.core.book.domain.Book;
import com.book.core.product.application.command.ProductListCursor;
import com.book.core.product.application.command.ProductListSort;
import com.book.core.product.application.port.ProductRepositoryPort;
import com.book.core.product.application.result.ProductListItem;
import com.book.core.product.application.usecase.FindProductsByIdsUseCase;
import com.book.core.product.domain.Product;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class GetCartUseCaseTest {
    private final FakeCartRepository cartRepository = new FakeCartRepository();
    private final FakeCartItemRepository cartItemRepository = new FakeCartItemRepository();
    private final FakeProductRepository productRepository = new FakeProductRepository();
    private final GetCartUseCase useCase =
        new GetCartUseCase(cartRepository, cartItemRepository, new FindProductsByIdsUseCase(productRepository));

    @Test
    void 장바구니가_없으면_빈_items를_반환한다() {
        final GetCartResult result = useCase.execute(42L);

        assertThat(result.items()).isEmpty();
        assertThat(cartItemRepository.requestedCartId).isNull();
        assertThat(productRepository.requestedProductIds).isNull();
    }

    @Test
    void 활성_장바구니_항목을_저장소가_정렬한_순서와_필드로_반환한다() {
        cartRepository.cart = new Cart(7L, 42L);
        cartItemRepository.items = List.of(new CartItem(11L, 7L, 200L, 2), new CartItem(10L, 7L, 100L, 1));

        final GetCartResult result = useCase.execute(42L);

        assertThat(cartItemRepository.requestedCartId).isEqualTo(7L);
        assertThat(result.items()).extracting(GetCartItemResult::cartItemId, GetCartItemResult::productId, GetCartItemResult::quantity)
            .containsExactly(org.assertj.core.groups.Tuple.tuple(11L, 200L, 2), org.assertj.core.groups.Tuple.tuple(10L, 100L, 1));
    }

    @Test
    void 상품_정보를_한번에_조회하고_비활성_품절_고아_항목을_유지한다() {
        cartRepository.cart = new Cart(7L, 42L);
        cartItemRepository.items = List.of(new CartItem(21L, 7L, 101L, 2), new CartItem(22L, 7L, 102L, 2), new CartItem(23L, 7L, 103L, 1),
            new CartItem(24L, 7L, 104L, 1), new CartItem(25L, 7L, 999L, 1));
        final Product activeProduct = product(101L, "활성 상품", null, 2);
        final Product lowStockProduct = product(102L, "재고 부족 상품", "low-stock.jpg", 1);
        final Product deletedProduct = product(103L, "삭제 상품", "deleted.jpg", 10);
        deletedProduct.delete();
        final Product deletedBookProduct = product(104L, "삭제 도서 상품", "deleted-book.jpg", 10);
        deletedBookProduct.book().delete();
        productRepository.products = List.of(activeProduct, lowStockProduct, deletedProduct, deletedBookProduct);

        final GetCartResult result = useCase.execute(42L);

        assertThat(productRepository.requestedProductIds).containsExactly(101L, 102L, 103L, 104L, 999L);
        assertThat(result.items()).extracting(GetCartItemResult::cartItemId, GetCartItemResult::productId, GetCartItemResult::quantity,
            GetCartItemResult::isAvailableForPurchase).containsExactly(org.assertj.core.groups.Tuple.tuple(21L, 101L, 2, true),
                org.assertj.core.groups.Tuple.tuple(22L, 102L, 2, false), org.assertj.core.groups.Tuple.tuple(23L, 103L, 1, false),
                org.assertj.core.groups.Tuple.tuple(24L, 104L, 1, false), org.assertj.core.groups.Tuple.tuple(25L, 999L, 1, false));
        assertThat(result.items().get(0).salePrice()).isEqualByComparingTo("20000.00");
        assertThat(result.items().get(0).discountedPrice()).isEqualByComparingTo("18000.00");
        assertThat(result.items().get(0).thumbnailUrl()).isNull();
        assertThat(result.items().get(1).itemName()).isEqualTo("재고 부족 상품");
        assertThat(result.items().get(1).salePrice()).isEqualByComparingTo("20000.00");
        assertThat(result.items().get(2).itemName()).isEqualTo("삭제 상품");
        assertThat(result.items().get(2).thumbnailUrl()).isEqualTo("deleted.jpg");
        assertThat(result.items().get(2).salePrice()).isNull();
        assertThat(result.items().get(2).discountedPrice()).isNull();
        assertThat(result.items().get(3).itemName()).isEqualTo("삭제 도서 상품");
        assertThat(result.items().get(3).salePrice()).isNull();
        assertThat(result.items().get(4).itemName()).isNull();
        assertThat(result.items().get(4).thumbnailUrl()).isNull();
        assertThat(result.items().get(4).salePrice()).isNull();
        assertThat(result.items().get(4).discountedPrice()).isNull();
    }

    private static Product product(final long id, final String name, final String thumbnailUrl, final int stockQuantity) {
        final Book book = new Book(id + 1000L, null, null, "도서명", "작가", null, "출판사", "소설", LocalDate.of(2026, 1, 1), null);
        return new Product(id, book, name, thumbnailUrl, new BigDecimal("20000.00"), new BigDecimal("18000.00"), new BigDecimal("12000.00"),
            stockQuantity);
    }

    private static final class FakeProductRepository implements ProductRepositoryPort {
        private List<Product> products = List.of();
        private List<Long> requestedProductIds;

        @Override
        public List<Product> findByIds(final List<Long> productIds) {
            requestedProductIds = productIds;
            return products.stream().filter((final var product) -> productIds.contains(product.id())).toList();
        }

        @Override
        public Optional<Product> findActiveById(final Long productId) {
            return Optional.empty();
        }

        @Override
        public Optional<Product> findActiveByBookId(final Long bookId) {
            return Optional.empty();
        }

        @Override
        public List<Product> findActiveByBookIds(final List<Long> bookIds) {
            return List.of();
        }

        @Override
        public List<ProductListItem> findActiveProducts(final Long categoryId, final LocalDate publishedFrom, final LocalDate publishedTo,
            final ProductListSort sort, final ProductListCursor cursor, final int limit) {
            return List.of();
        }
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
