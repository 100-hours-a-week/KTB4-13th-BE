package com.book.core.recommendation.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import com.book.core.book.application.port.BookRepositoryPort;
import com.book.core.book.application.usecase.GetBooksUseCase;
import com.book.core.book.domain.Book;
import com.book.core.product.application.port.ProductRepositoryPort;
import com.book.core.product.application.usecase.FindProductByBookIdUseCase;
import com.book.core.product.domain.Product;
import com.book.core.recommendation.application.command.GetRecommendationCardCommand;
import com.book.core.recommendation.application.port.RecommendationCardRepositoryPort;
import com.book.core.recommendation.domain.RecommendationCard;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class GetRecommendationCardUseCaseTest {
    private final FakeRecommendationCardRepository cardRepository = new FakeRecommendationCardRepository();
    private final FakeBookRepositoryPort bookRepository = new FakeBookRepositoryPort();
    private final FakeProductRepositoryPort productRepository = new FakeProductRepositoryPort();
    private final GetRecommendationCardUseCase useCase = new GetRecommendationCardUseCase(cardRepository,
        new GetBooksUseCase(bookRepository), new FindProductByBookIdUseCase(productRepository));

    @Test
    void 소유자의_카드_상세를_Book과_Product_정보로_조회한다() {
        cardRepository.cards.put(1L, RecommendationCard.restore(1L, 42L, 10L, "이유", null));
        bookRepository.books.put(10L, book(10L));
        productRepository.products.put(10L, product(10L, new BigDecimal("18000.00"), new BigDecimal("20000.00")));

        final var result = useCase.execute(new GetRecommendationCardCommand(42L, 1L));

        assertThat(result.recommendationCardId()).isEqualTo(1L);
        assertThat(result.bookId()).isEqualTo(10L);
        assertThat(result.title()).isEqualTo("제목10");
        assertThat(result.productId()).isEqualTo(10L);
        assertThat(result.price()).isEqualByComparingTo("18000.00");
    }

    @Test
    void Product가_없어도_상세_조회는_성공하고_productId는_null이다() {
        cardRepository.cards.put(1L, RecommendationCard.restore(1L, 42L, 10L, "이유", null));
        bookRepository.books.put(10L, book(10L));

        final var result = useCase.execute(new GetRecommendationCardCommand(42L, 1L));

        assertThat(result.productId()).isNull();
        assertThat(result.price()).isNull();
    }

    @Test
    void 존재하지_않는_카드는_예외를_던진다() {
        assertThatThrownBy(() -> useCase.execute(new GetRecommendationCardCommand(42L, 999L))).isInstanceOf(CoreException.class)
            .satisfies(exception -> assertThat(((CoreException) exception).errorCode()).isEqualTo(ErrorCode.RECOMMENDATION_CARD_NOT_FOUND));
    }

    @Test
    void 다른_사용자의_카드는_존재하지_않는_카드와_동일하게_거부한다() {
        cardRepository.cards.put(1L, RecommendationCard.restore(1L, 42L, 10L, "이유", null));
        bookRepository.books.put(10L, book(10L));

        assertThatThrownBy(() -> useCase.execute(new GetRecommendationCardCommand(99L, 1L))).isInstanceOf(CoreException.class)
            .satisfies(exception -> assertThat(((CoreException) exception).errorCode()).isEqualTo(ErrorCode.RECOMMENDATION_CARD_NOT_FOUND));
    }

    private Book book(final long id) {
        return new Book(id, null, null, "제목" + id, "작가", null, "출판사", "소설", LocalDate.of(2026, 1, 1), null);
    }

    private Product product(final long bookId, final BigDecimal discountedPrice, final BigDecimal salePrice) {
        return new Product(bookId, book(bookId), "상품명", null, salePrice, discountedPrice, new BigDecimal("10000.00"), 10);
    }

    private static class FakeRecommendationCardRepository implements RecommendationCardRepositoryPort {
        final Map<Long, RecommendationCard> cards = new HashMap<>();

        @Override
        public RecommendationCard save(final RecommendationCard card) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<RecommendationCard> saveAll(final List<RecommendationCard> cards) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Optional<RecommendationCard> findById(final Long id) {
            return Optional.ofNullable(cards.get(id));
        }
    }

    private static class FakeBookRepositoryPort implements BookRepositoryPort {
        final Map<Long, Book> books = new HashMap<>();

        @Override
        public List<Book> findAllByIdIn(final List<Long> ids) {
            return ids.stream().map(books::get).filter(Objects::nonNull).toList();
        }
    }

    private static class FakeProductRepositoryPort implements ProductRepositoryPort {
        final Map<Long, Product> products = new HashMap<>();

        @Override
        public Optional<com.book.core.product.domain.Product> findActiveById(final Long productId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Optional<Product> findActiveByBookId(final Long bookId) {
            return Optional.ofNullable(products.get(bookId));
        }

        @Override
        public List<com.book.core.product.application.result.ProductListItem> findActiveProducts(final Long categoryId,
            final LocalDate publishedFrom, final LocalDate publishedTo,
            final com.book.core.product.application.command.ProductListSort sort,
            final com.book.core.product.application.command.ProductListCursor cursor, final int limit) {
            throw new UnsupportedOperationException();
        }
    }
}
