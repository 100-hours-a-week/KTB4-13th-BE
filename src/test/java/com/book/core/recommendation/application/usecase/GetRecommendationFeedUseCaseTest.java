package com.book.core.recommendation.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;

import com.book.core.book.domain.Book;
import com.book.core.product.application.command.ProductListCursor;
import com.book.core.product.application.command.ProductListSort;
import com.book.core.product.application.port.ProductRepositoryPort;
import com.book.core.product.application.result.ProductListItem;
import com.book.core.product.application.usecase.FindProductIdsByBookIdsUseCase;
import com.book.core.product.domain.Product;
import com.book.core.recommendation.application.command.GetRecommendationFeedCommand;
import com.book.core.recommendation.application.command.RecommendationFeedSort;
import com.book.core.recommendation.application.command.RecommendationFeedSurface;
import com.book.core.recommendation.application.port.RecommendationFeedClient;
import com.book.core.recommendation.application.port.RecommendationFeedItem;
import com.book.core.recommendation.application.port.RecommendationFeedRequest;
import com.book.core.recommendation.application.port.RecommendationFeedResult;
import com.book.core.recommendation.application.result.GetRecommendationFeedResult;
import com.book.core.recommendation.application.result.RecommendationFeedItemResult;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class GetRecommendationFeedUseCaseTest {
    private final FakeRecommendationFeedClient recommendationFeedClient = new FakeRecommendationFeedClient();
    private final FakeProductRepository productRepository = new FakeProductRepository();
    private final GetRecommendationFeedUseCase useCase =
        new GetRecommendationFeedUseCase(recommendationFeedClient, new FindProductIdsByBookIdsUseCase(productRepository));

    @Test
    void 피드_조건을_그대로_AI_피드_요청으로_전달한다() {
        useCase.execute(new GetRecommendationFeedCommand(42L, RecommendationFeedSurface.RECOMMEND_MORE, 20, "abc",
            RecommendationFeedSort.NEWEST, "소설", 2020, 2024, 70));

        assertThat(recommendationFeedClient.request).isEqualTo(new RecommendationFeedRequest(42L, RecommendationFeedSurface.RECOMMEND_MORE,
            20, "abc", RecommendationFeedSort.NEWEST, "소설", 2020, 2024, 70));
    }

    @Test
    void 각_피드_항목에_도서의_활성_상품_productId를_한_번에_조회해_채우고_AI_피드_값을_보존한다() {
        productRepository.productsByBookId.put(3310L, product(601L, 3310L));
        productRepository.productsByBookId.put(2077L, product(602L, 2077L));
        recommendationFeedClient.result = new RecommendationFeedResult(
            List.of(feedItem(3310L, "아무튼, 산", 84), feedItem(2077L, "여행의 이유", 79)), "next-page", false, "rule-only");

        final GetRecommendationFeedResult result = useCase.execute(homeCommand());

        assertThat(result.items()).containsExactly(itemResult(3310L, 601L, "아무튼, 산", 84), itemResult(2077L, 602L, "여행의 이유", 79));
        assertThat(result.nextCursor()).isEqualTo("next-page");
        assertThat(result.coldStart()).isFalse();
        assertThat(result.degraded()).isEqualTo("rule-only");
        assertThat(productRepository.requestedBookIdsPerLookup).containsExactly(List.of(3310L, 2077L));
    }

    @Test
    void 활성_상품이_없는_도서의_피드_항목은_productId를_null로_반환한다() {
        productRepository.productsByBookId.put(3310L, product(601L, 3310L));
        recommendationFeedClient.result =
            new RecommendationFeedResult(List.of(feedItem(3310L, "아무튼, 산", 84), feedItem(4400L, "상품 없는 책", 60)), null, false, null);

        final GetRecommendationFeedResult result = useCase.execute(homeCommand());

        assertThat(result.items()).containsExactly(itemResult(3310L, 601L, "아무튼, 산", 84), itemResult(4400L, null, "상품 없는 책", 60));
    }

    @Test
    void 피드가_비어_있으면_상품을_조회하지_않고_coldStart를_그대로_반환한다() {
        recommendationFeedClient.result = new RecommendationFeedResult(List.of(), null, true, null);

        final GetRecommendationFeedResult result = useCase.execute(homeCommand());

        assertThat(result.items()).isEmpty();
        assertThat(result.coldStart()).isTrue();
        assertThat(productRepository.requestedBookIdsPerLookup).isEmpty();
    }

    private static GetRecommendationFeedCommand homeCommand() {
        return new GetRecommendationFeedCommand(42L, RecommendationFeedSurface.HOME, 15, null, null, null, null, null, null);
    }

    private static RecommendationFeedItem feedItem(final long bookId, final String title, final int matchScore) {
        return new RecommendationFeedItem(bookId, title, "작가", new BigDecimal("9900"), "https://example.com/" + bookId + ".jpg", true,
            matchScore);
    }

    private static RecommendationFeedItemResult itemResult(final long bookId, final Long productId, final String title,
        final int matchScore) {
        return new RecommendationFeedItemResult(bookId, productId, title, "작가", new BigDecimal("9900"),
            "https://example.com/" + bookId + ".jpg", true, matchScore);
    }

    private static Product product(final long productId, final long bookId) {
        final Book book = new Book(bookId, null, null, "제목", "작가", null, "출판사", "소설", LocalDate.of(2026, 1, 1), null);
        return new Product(productId, book, "상품명", null, new BigDecimal("20000.00"), new BigDecimal("18000.00"), new BigDecimal("12000.00"),
            10);
    }

    private static final class FakeRecommendationFeedClient implements RecommendationFeedClient {
        private RecommendationFeedResult result = new RecommendationFeedResult(List.of(), null, false, null);
        private RecommendationFeedRequest request;

        @Override
        public RecommendationFeedResult getFeed(final RecommendationFeedRequest request) {
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
