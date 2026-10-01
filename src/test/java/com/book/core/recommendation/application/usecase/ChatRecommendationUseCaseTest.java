package com.book.core.recommendation.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;

import com.book.core.book.application.port.BookRepositoryPort;
import com.book.core.book.application.usecase.GetBooksUseCase;
import com.book.core.book.domain.Book;
import com.book.core.product.application.command.ProductListCursor;
import com.book.core.product.application.command.ProductListSort;
import com.book.core.product.application.port.ProductRepositoryPort;
import com.book.core.product.application.result.ProductListItem;
import com.book.core.product.application.usecase.FindProductByBookIdUseCase;
import com.book.core.product.domain.Product;
import com.book.core.recommendation.application.command.ChatRecommendationCommand;
import com.book.core.recommendation.application.port.AiRecommendationCard;
import com.book.core.recommendation.application.port.AiRecommendationChatRequest;
import com.book.core.recommendation.application.port.AiRecommendationChatResult;
import com.book.core.recommendation.application.port.AiRecommendationClient;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class ChatRecommendationUseCaseTest {
    private final FakeAiRecommendationClient aiRecommendationClient = new FakeAiRecommendationClient();
    private final FakeBookRepositoryPort bookRepository = new FakeBookRepositoryPort();
    private final FakeProductRepositoryPort productRepository = new FakeProductRepositoryPort();
    private final GetBooksUseCase getBooksUseCase = new GetBooksUseCase(bookRepository);
    private final ChatRecommendationUseCase useCase =
        new ChatRecommendationUseCase(aiRecommendationClient, getBooksUseCase, new FindProductByBookIdUseCase(productRepository));

    @Test
    void AI_카드를_Book_정보와_활성_상품의_ID와_가격으로_보강하고_AI_추천_값을_보존한다() {
        bookRepository.books.put(1L, book(1L));
        productRepository.productsByBookId.put(1L, product(501L, 1L));
        aiRecommendationClient.result = new AiRecommendationChatResult(Map.of("intent", "semantic"), "reply",
            List.of(new AiRecommendationCard(1L, 87, "한 줄 이유", "긴 이유")), "followup", List.of("btn"), false);

        final var outcome = useCase.execute(command(Map.of("intent", "semantic"), List.of()));

        assertThat(outcome.cards()).hasSize(1);
        final var card = outcome.cards().getFirst();
        assertThat(card.bookId()).isEqualTo(1L);
        assertThat(card.productId()).isEqualTo(501L);
        assertThat(card.title()).isEqualTo("제목1");
        assertThat(card.price()).isEqualByComparingTo("13500");
        assertThat(card.matchScore()).isEqualTo(87);
        assertThat(card.reasonShort()).isEqualTo("한 줄 이유");
        assertThat(card.reasonLong()).isEqualTo("긴 이유");
        assertThat(outcome.reply()).isEqualTo("reply");
        assertThat(outcome.degraded()).isFalse();
    }

    @Test
    void 활성_상품이_없는_Book의_카드는_productId와_price를_null로_반환한다() {
        bookRepository.books.put(1L, book(1L));
        aiRecommendationClient.result = new AiRecommendationChatResult(Map.of(), "reply",
            List.of(new AiRecommendationCard(1L, 70, "한 줄 이유", null)), null, List.of(), true);

        final var outcome = useCase.execute(command(Map.of(), List.of()));

        assertThat(outcome.cards().getFirst().productId()).isNull();
        assertThat(outcome.cards().getFirst().price()).isNull();
        assertThat(outcome.cards().getFirst().reasonLong()).isNull();
        assertThat(outcome.degraded()).isTrue();
    }

    @Test
    void 인증된_사용자_ID로_AI를_호출한다() {
        aiRecommendationClient.result = new AiRecommendationChatResult(Map.of(), "reply", List.of(), null, List.of(), false);

        useCase.execute(command(Map.of(), List.of(7L)));

        assertThat(aiRecommendationClient.request.userId()).isEqualTo(42L);
        assertThat(aiRecommendationClient.request.excludeBookIds()).containsExactly(7L);
    }

    @Test
    void 존재하지_않는_Book의_카드는_제외한다() {
        aiRecommendationClient.result = new AiRecommendationChatResult(Map.of(), "reply",
            List.of(new AiRecommendationCard(999L, 50, "이유", "이유")), null, List.of(), false);

        final var outcome = useCase.execute(command(Map.of(), List.of()));

        assertThat(outcome.cards()).isEmpty();
    }

    @Test
    void 카드는_최대_3개까지만_반환한다() {
        bookRepository.books.put(1L, book(1L));
        bookRepository.books.put(2L, book(2L));
        bookRepository.books.put(3L, book(3L));
        bookRepository.books.put(4L, book(4L));
        aiRecommendationClient.result =
            new AiRecommendationChatResult(Map.of(), "reply", List.of(card(1L), card(2L), card(3L), card(4L)), null, List.of(), false);

        final var outcome = useCase.execute(command(Map.of(), List.of()));

        assertThat(outcome.cards()).hasSize(3);
    }

    private ChatRecommendationCommand command(final Map<String, Object> spec, final List<Long> exclude) {
        return new ChatRecommendationCommand(42L, true, spec, "메시지", List.of(), exclude);
    }

    private AiRecommendationCard card(final long bookId) {
        return new AiRecommendationCard(bookId, 80, "이유" + bookId, null);
    }

    private Book book(final long id) {
        return new Book(id, null, null, "제목" + id, "작가", null, "출판사", "소설", LocalDate.of(2026, 1, 1), null);
    }

    private Product product(final long productId, final long bookId) {
        return new Product(productId, book(bookId), "상품명", null, new BigDecimal("15000.00"), new BigDecimal("13500.00"),
            new BigDecimal("10000.00"), 10);
    }

    private static class FakeAiRecommendationClient implements AiRecommendationClient {
        AiRecommendationChatResult result;
        AiRecommendationChatRequest request;

        @Override
        public AiRecommendationChatResult chat(final AiRecommendationChatRequest request) {
            this.request = request;
            return result;
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
        final Map<Long, Product> productsByBookId = new HashMap<>();

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
            return Optional.ofNullable(productsByBookId.get(bookId));
        }

        @Override
        public List<Product> findActiveByBookIds(final List<Long> bookIds) {
            return List.of();
        }

        @Override
        public List<ProductListItem> findActiveProducts(final Long categoryId, final LocalDate publishedFrom, final LocalDate publishedTo,
            final ProductListSort sort, final ProductListCursor cursor, final int limit) {
            throw new UnsupportedOperationException();
        }
    }
}
