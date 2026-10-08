package com.book.core.search.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;

import com.book.core.book.domain.Book;
import com.book.core.product.application.command.ProductListCursor;
import com.book.core.product.application.command.ProductListSort;
import com.book.core.product.application.port.ProductRepositoryPort;
import com.book.core.product.application.result.ProductListItem;
import com.book.core.product.application.usecase.FindProductIdsByBookIdsUseCase;
import com.book.core.product.domain.Product;
import com.book.core.search.application.command.BookSearchSort;
import com.book.core.search.application.command.SearchBooksCommand;
import com.book.core.search.application.port.BookSearchClient;
import com.book.core.search.application.port.BookSearchItem;
import com.book.core.search.application.port.BookSearchRequest;
import com.book.core.search.application.port.BookSearchResult;
import com.book.core.search.application.result.SearchBookItemResult;
import com.book.core.search.application.result.SearchBooksResult;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class SearchBooksUseCaseTest {
    private final FakeBookSearchClient bookSearchClient = new FakeBookSearchClient();
    private final FakeProductRepository productRepository = new FakeProductRepository();
    private final SearchBooksUseCase useCase =
        new SearchBooksUseCase(bookSearchClient, new FindProductIdsByBookIdsUseCase(productRepository));

    @Test
    void 검색_조건을_그대로_AI_검색_요청으로_전달한다() {
        useCase.execute(new SearchBooksCommand("투자 입문", "경제경영", 10000, 20000, 2020, 2024, BookSearchSort.NEWEST, "abc", 20));

        assertThat(bookSearchClient.request)
            .isEqualTo(new BookSearchRequest("투자 입문", "경제경영", 10000, 20000, 2020, 2024, BookSearchSort.NEWEST, "abc", 20));
    }

    @Test
    void 각_검색_결과에_도서의_활성_상품_productId를_한_번에_조회해_채우고_AI_검색_값을_보존한다() {
        productRepository.productsByBookId.put(2077L, product(501L, 2077L));
        productRepository.productsByBookId.put(3310L, product(502L, 3310L));
        bookSearchClient.result =
            new BookSearchResult(List.of(searchItem(2077L, "여행의 이유"), searchItem(3310L, "아무튼, 산")), "next-page", null, "keyword-only");

        final SearchBooksResult result = useCase.execute(command());

        assertThat(result.items()).containsExactly(itemResult(2077L, 501L, "여행의 이유"), itemResult(3310L, 502L, "아무튼, 산"));
        assertThat(result.nextCursor()).isEqualTo("next-page");
        assertThat(result.fallbackMessage()).isNull();
        assertThat(result.degraded()).isEqualTo("keyword-only");
        assertThat(productRepository.requestedBookIdsPerLookup).containsExactly(List.of(2077L, 3310L));
    }

    @Test
    void 활성_상품이_없는_도서의_검색_결과는_productId를_null로_반환한다() {
        productRepository.productsByBookId.put(2077L, product(501L, 2077L));
        bookSearchClient.result =
            new BookSearchResult(List.of(searchItem(2077L, "여행의 이유"), searchItem(4400L, "상품 없는 책")), null, null, null);

        final SearchBooksResult result = useCase.execute(command());

        assertThat(result.items()).containsExactly(itemResult(2077L, 501L, "여행의 이유"), itemResult(4400L, null, "상품 없는 책"));
    }

    @Test
    void 검색_결과가_없으면_상품을_조회하지_않고_fallback_문구를_그대로_반환한다() {
        bookSearchClient.result = new BookSearchResult(List.of(), null, "원하는 책을 못 찾았어요. AI 추천에게 물어볼까요?", "keyword-only");

        final SearchBooksResult result = useCase.execute(command());

        assertThat(result.items()).isEmpty();
        assertThat(result.fallbackMessage()).isEqualTo("원하는 책을 못 찾았어요. AI 추천에게 물어볼까요?");
        assertThat(result.degraded()).isEqualTo("keyword-only");
        assertThat(productRepository.requestedBookIdsPerLookup).isEmpty();
    }

    private static SearchBooksCommand command() {
        return new SearchBooksCommand("여행", null, null, null, null, null, BookSearchSort.POPULAR, null, 12);
    }

    private static BookSearchItem searchItem(final long bookId, final String title) {
        return new BookSearchItem(bookId, title, "작가", "출판사", new BigDecimal("13500"), true, "https://example.com/" + bookId + ".jpg");
    }

    private static SearchBookItemResult itemResult(final long bookId, final Long productId, final String title) {
        return new SearchBookItemResult(bookId, productId, title, "작가", "출판사", new BigDecimal("13500"), true,
            "https://example.com/" + bookId + ".jpg");
    }

    private static Product product(final long productId, final long bookId) {
        final Book book = new Book(bookId, null, null, "제목", "작가", null, "출판사", "소설", LocalDate.of(2026, 1, 1), null);
        return new Product(productId, book, "상품명", null, new BigDecimal("20000.00"), new BigDecimal("18000.00"), new BigDecimal("12000.00"),
            10);
    }

    private static final class FakeBookSearchClient implements BookSearchClient {
        private BookSearchResult result = new BookSearchResult(List.of(), null, null, null);
        private BookSearchRequest request;

        @Override
        public BookSearchResult search(final BookSearchRequest request) {
            this.request = request;
            return result;
        }
    }

    private static final class FakeProductRepository implements ProductRepositoryPort {
        private final Map<Long, Product> productsByBookId = new HashMap<>();
        private final List<List<Long>> requestedBookIdsPerLookup = new ArrayList<>();

        @Override
        public List<Product> findByIds(final List<Long> productIds) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Optional<Product> findActiveById(final Long productId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Optional<Product> findActiveByBookId(final Long bookId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<Product> findActiveByBookIds(final List<Long> bookIds) {
            requestedBookIdsPerLookup.add(bookIds);
            return bookIds.stream().map(productsByBookId::get).filter(Objects::nonNull).toList();
        }

        @Override
        public List<ProductListItem> findActiveProducts(final Long categoryId, final LocalDate publishedFrom, final LocalDate publishedTo,
            final ProductListSort sort, final ProductListCursor cursor, final int limit) {
            throw new UnsupportedOperationException();
        }
    }
}
