package com.book.core.onboarding.infrastructure.persistence.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.book.core.onboarding.application.port.AiPersonalizationProfileHistory;
import com.book.core.onboarding.application.port.AiPersonalizationProfileMemory;
import com.book.core.onboarding.application.port.ProfileHistoryRepositoryPort;
import com.book.core.onboarding.application.port.ProfileMemoryRepositoryPort;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
@Tag("integration")
class ProfileRepositoryAdaptersIntegrationTest {
    private static final AtomicLong NEXT_ID = new AtomicLong(9_870_000L);
    private static final LocalDateTime WINDOW_START = LocalDateTime.of(2026, 10, 19, 0, 0);
    private static final LocalDateTime WINDOW_END = LocalDateTime.of(2026, 10, 20, 0, 0);

    @Autowired
    ProfileHistoryRepositoryPort historyRepository;

    @Autowired
    ProfileMemoryRepositoryPort memoryRepository;

    @Autowired
    JdbcTemplate jdbc;

    Long userId;

    @BeforeEach
    void setUp() {
        userId = insertUser();
    }

    @Test
    void 결제_후_취소되지_않은_구매와_나의_도서관과_지우지_않은_리뷰를_ISBN으로_조회한다() {
        final long paid = insertProduct("9788900000011");
        final long partiallyCanceled = insertProduct("9788900000028");
        final long canceled = insertProduct("9788900000035");
        final long unpaid = insertProduct("9788900000042");
        final long withoutIsbn = insertProduct(null);
        insertOrderItem(insertOrder("PAID", WINDOW_START), paid, "PAID");
        insertOrderItem(insertOrder("PARTIAL_CANCELED", WINDOW_START), partiallyCanceled, "PARTIAL_CANCELED");
        insertOrderItem(insertOrder("CANCELED", WINDOW_START), canceled, "CANCELED");
        insertOrderItem(insertOrder("CREATED", WINDOW_START), unpaid, "CREATED");
        insertOrderItem(insertOrder("PAID", WINDOW_START), withoutIsbn, "PAID");
        insertLibraryBook(bookIdOf(canceled), null, WINDOW_START);
        insertLibraryBook(bookIdOf(unpaid), LocalDateTime.of(2026, 10, 19, 1, 0), WINDOW_START);
        insertReview(paid, "4.5", null, WINDOW_START);
        insertReview(canceled, "1.0", LocalDateTime.of(2026, 10, 19, 1, 0), WINDOW_START);

        final AiPersonalizationProfileHistory history = historyRepository.findHistory(userId);

        assertThat(history.purchasedIsbns()).containsExactly("9788900000011", "9788900000028");
        assertThat(history.libraryIsbns()).containsExactly("9788900000035");
        assertThat(history.reviews())
            .containsExactly(new AiPersonalizationProfileHistory.Review("9788900000011", new BigDecimal("4.5"), "리뷰"));
    }

    @Test
    void 기간_안에_구매_나의_도서관_리뷰가_바뀐_회원만_조회한다() {
        final long product = insertProduct("9788900000059");
        final Long orderUser = userId;
        final Long libraryUser = insertUser();
        final Long reviewUser = insertUser();
        final Long outsideUser = insertUser();
        insertOrderItem(insertOrderOf(orderUser, "PARTIAL_CANCELED", WINDOW_START.plusHours(3)), product, "PARTIAL_CANCELED");
        insertLibraryBookOf(libraryUser, bookIdOf(product), WINDOW_END.minusSeconds(1));
        insertReviewOf(reviewUser, product, WINDOW_START);
        insertLibraryBookOf(outsideUser, bookIdOf(product), WINDOW_END);

        final List<Long> changed = historyRepository.findChangedUserIds(instant(WINDOW_START), instant(WINDOW_END));

        assertThat(changed).contains(orderUser, libraryUser, reviewUser).doesNotContain(outsideUser);
    }

    @Test
    void 지우지_않은_취향_기억을_최근순으로_제한해_조회하고_저장_시각을_한국_시간으로_바꾼다() {
        insertMemory("추리 소설", LocalDateTime.of(2026, 10, 19, 15, 0, 0, 123_456_000), null);
        insertMemory("로맨스 소설", LocalDateTime.of(2026, 10, 19, 16, 0), null);
        insertMemory("김영하", LocalDateTime.of(2026, 10, 19, 17, 0), LocalDateTime.of(2026, 10, 19, 18, 0));

        final List<AiPersonalizationProfileMemory> memories = memoryRepository.findLatestActive(userId, 1);

        assertThat(memories).singleElement().satisfies((final AiPersonalizationProfileMemory memory) -> {
            assertThat(memory.keyword()).isEqualTo("로맨스 소설");
            assertThat(memory.type()).isEqualTo("topic");
            assertThat(memory.createdAt()).isEqualTo(LocalDateTime.of(2026, 10, 19, 16, 0).atZone(ZoneId.systemDefault())
                .withZoneSameInstant(ZoneId.of("Asia/Seoul")).toLocalDateTime());
        });
        assertThat(memoryRepository.findLatestActive(userId, 500)).hasSize(2);
    }

    private Long insertUser() {
        final long id = NEXT_ID.incrementAndGet();
        jdbc.update("INSERT INTO users (id, nickname) VALUES (?, ?)", id, "profile" + id);
        return id;
    }

    private long insertProduct(final String isbn13) {
        final long id = NEXT_ID.incrementAndGet();
        jdbc.update("""
            INSERT INTO books (id, isbn13, title, author, publisher, category, published_at, created_at, updated_at)
            VALUES (?, ?, '제목', '작가', '출판사', '소설', DATE '2026-01-01', CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6))
            """, id, isbn13);
        jdbc.update("""
            INSERT INTO products (id, book_id, name, sale_price, discounted_price, cost_price, stock_quantity, created_at, updated_at)
            VALUES (?, ?, '상품', 20000.00, 18000.00, 12000.00, 10, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6))
            """, id, id);
        return id;
    }

    private long bookIdOf(final long productId) {
        return jdbc.queryForObject("SELECT book_id FROM products WHERE id = ?", Long.class, productId);
    }

    private long insertOrder(final String status, final LocalDateTime updatedAt) {
        return insertOrderOf(userId, status, updatedAt);
    }

    private long insertOrderOf(final Long ownerId, final String status, final LocalDateTime updatedAt) {
        final long id = NEXT_ID.incrementAndGet();
        final LocalDateTime paidAt = "CREATED".equals(status) ? null : updatedAt;
        jdbc.update("""
            INSERT INTO orders (id, user_id, `key`, total_price, status, paid_at, created_at, updated_at)
            VALUES (?, ?, ?, 100, ?, ?, ?, ?)
            """, id, ownerId, "profile-order-" + id, status, paidAt, updatedAt, updatedAt);
        return id;
    }

    private void insertOrderItem(final long orderId, final long productId, final String status) {
        final LocalDateTime updatedAt = jdbc.queryForObject("SELECT updated_at FROM orders WHERE id = ?", LocalDateTime.class, orderId);
        jdbc.update("""
            INSERT INTO order_item (id, order_id, product_id, unit_price, total_price, quantity, status, created_at, updated_at)
            VALUES (?, ?, ?, 100, 100, 1, ?, ?, ?)
            """, NEXT_ID.incrementAndGet(), orderId, productId, status, updatedAt, updatedAt);
    }

    private void insertLibraryBook(final long bookId, final LocalDateTime deletedAt, final LocalDateTime updatedAt) {
        jdbc.update("""
            INSERT INTO user_library_books (user_id, book_id, added_at, deleted_at, created_at, updated_at)
            VALUES (?, ?, ?, ?, ?, ?)
            """, userId, bookId, updatedAt, deletedAt, updatedAt, updatedAt);
    }

    private void insertLibraryBookOf(final Long ownerId, final long bookId, final LocalDateTime updatedAt) {
        jdbc.update("""
            INSERT INTO user_library_books (user_id, book_id, added_at, created_at, updated_at)
            VALUES (?, ?, ?, ?, ?)
            """, ownerId, bookId, updatedAt, updatedAt, updatedAt);
    }

    private void insertReview(final long productId, final String rate, final LocalDateTime deletedAt, final LocalDateTime updatedAt) {
        final long id = NEXT_ID.incrementAndGet();
        jdbc.update("""
            INSERT INTO reviews (id, user_id, review_key, target_type, target_id, rate, content, deleted_at, created_at, updated_at)
            VALUES (?, ?, ?, 'PRODUCT', ?, ?, '리뷰', ?, ?, ?)
            """, id, userId, "PROFILE_" + id, productId, new BigDecimal(rate), deletedAt, updatedAt, updatedAt);
    }

    private void insertReviewOf(final Long ownerId, final long productId, final LocalDateTime updatedAt) {
        final long id = NEXT_ID.incrementAndGet();
        jdbc.update("""
            INSERT INTO reviews (id, user_id, review_key, target_type, target_id, rate, content, created_at, updated_at)
            VALUES (?, ?, ?, 'PRODUCT', ?, 4.0, '리뷰', ?, ?)
            """, id, ownerId, "PROFILE_" + id, productId, updatedAt, updatedAt);
    }

    private void insertMemory(final String keyword, final LocalDateTime createdAt, final LocalDateTime deletedAt) {
        jdbc.update("""
            INSERT INTO user_memories (user_id, type, value, strength, categories, keyword, vector, dim, conversation_id,
                created_at, updated_at, deleted_at)
            VALUES (?, 'TOPIC', ?, 2, '[]' FORMAT JSON, ?, '[0.1]' FORMAT JSON, 1, '1', ?, ?, ?)
            """, userId, keyword + "을 좋아함", keyword, createdAt, createdAt, deletedAt);
    }

    private static Instant instant(final LocalDateTime value) {
        return value.atZone(ZoneId.systemDefault()).toInstant();
    }
}
