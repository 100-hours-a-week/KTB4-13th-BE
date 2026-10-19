package com.book.core.review.infrastructure.persistence.repository;

import com.book.core.review.domain.ReviewContent;
import com.book.core.review.domain.ReviewKey;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.book.core.order.application.port.OrderRepositoryPort;
import com.book.core.order.domain.Order;
import com.book.core.review.application.port.ReviewRepositoryPort;
import com.book.core.review.application.port.ReviewOrderItemRepositoryPort;
import java.time.LocalDateTime;
import com.book.core.review.domain.Review;
import com.book.core.review.domain.ReviewTarget;
import com.book.core.order.domain.OrderStatus;
import com.book.common.paging.OffsetLimit;
import com.book.core.review.domain.enums.ReviewTargetType;
import java.util.ArrayList;
import java.util.List;
import java.math.BigDecimal;
import java.sql.SQLException;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;

@SpringBootTest
@ActiveProfiles({"test", "mysql"})
@Testcontainers
@Tag("integration")
class ReviewRepositoryIntegrationTest {
    private static final int MYSQL_CHECK_CONSTRAINT_VIOLATED = 3819;

    @Container
    @ServiceConnection
    static final MySQLContainer mysql = new MySQLContainer("mysql:8.4.8");

    @Autowired
    ReviewRepositoryPort reviewRepository;

    @Autowired
    OrderRepositoryPort orderRepository;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    ReviewOrderItemRepositoryPort reviewOrders;

    @Test
    void 소프트_삭제한_리뷰도_같은_구매_키로_다시_작성할_수_없다() {
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM information_schema.columns "
            + "WHERE table_schema = DATABASE() AND table_name = 'reviews' AND column_name = 'order_item_id'", Integer.class)).isZero();
        insertBook(9301L);
        insertProduct(9401L, 9301L);
        final Order order = Order.create(42L, "review_persistence_test", null);
        order.addItem(9401L, "리뷰 테스트 상품", null, "저자", new BigDecimal("20.00"), new BigDecimal("17.25"), 1);
        final Order savedOrder = orderRepository.save(order);
        final Long orderItemId = jdbc.queryForObject("SELECT id FROM order_item WHERE order_id = ?", Long.class, savedOrder.id());
        final Review review = reviewRepository.save(Review.create(new ReviewKey(42L, "ORDER_ITEM_" + orderItemId),
            new ReviewTarget(ReviewTargetType.PRODUCT, 9401L), new ReviewContent(new BigDecimal("4.5"), "리뷰 본문", true)));

        assertThat(reviewRepository.findActiveById(review.id())).isPresent();
        assertThat(jdbc.queryForMap(
            "SELECT user_id, review_key, target_id, rate, content, is_spoiler, active_flag FROM reviews WHERE id = ?", review.id()))
            .containsEntry("user_id", 42L).containsEntry("review_key", "ORDER_ITEM_" + orderItemId).containsEntry("target_id", 9401L)
            .containsEntry("rate", new BigDecimal("4.5")).containsEntry("content", "리뷰 본문").containsEntry("is_spoiler", true)
            .containsEntry("active_flag", 1);
        assertThat(reviewRepository.existsByUserIdAndReviewKey(42L, "ORDER_ITEM_" + orderItemId)).isTrue();
        assertThatThrownBy(() -> reviewRepository.save(Review.create(new ReviewKey(42L, "ORDER_ITEM_" + orderItemId),
            new ReviewTarget(ReviewTargetType.PRODUCT, 9401L), new ReviewContent(new BigDecimal("5.0"), "중복", false))))
            .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> jdbc.update(
            "INSERT INTO reviews (user_id, review_key, target_type, target_id, rate, content, is_spoiler) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?)",
            43L, "ORDER_ITEM_" + orderItemId, "PRODUCT", 9401L, new BigDecimal("10.1"), "범위 초과", false))
            .isInstanceOf(DataAccessException.class).satisfies(exception -> {
                assertThat(exception.getCause()).isInstanceOf(SQLException.class);
                assertThat(((SQLException) exception.getCause()).getErrorCode()).isEqualTo(MYSQL_CHECK_CONSTRAINT_VIOLATED);
            });

        review.delete();
        reviewRepository.save(review);

        assertThat(reviewRepository.findActiveById(review.id())).isEmpty();
        assertThat(jdbc.queryForObject("SELECT active_flag FROM reviews WHERE id = ?", Integer.class, review.id())).isNull();
        assertThat(reviewRepository.existsByUserIdAndReviewKey(42L, "ORDER_ITEM_" + orderItemId)).isTrue();
        assertThat(reviewRepository.findUsedReviewKeys(42L, List.of("ORDER_ITEM_" + orderItemId)))
            .containsExactly("ORDER_ITEM_" + orderItemId);
        assertThatThrownBy(() -> reviewRepository.save(Review.create(new ReviewKey(42L, "ORDER_ITEM_" + orderItemId),
            new ReviewTarget(ReviewTargetType.PRODUCT, 9401L), new ReviewContent(new BigDecimal("5.0"), "삭제 후 재작성", false))))
            .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void 리뷰_조회는_대상과_삭제_여부를_필터링하고_요청한_offset부터_반환한다() {
        insertBook(9501L);
        insertBook(9502L);
        insertProduct(9601L, 9501L);
        insertProduct(9602L, 9502L);
        final List<Long> activeIds = new ArrayList<>();
        for (int index = 0; index < 6; index++) {
            activeIds.add(saveReview(9601L, "활성 " + index).id());
        }
        final var deleted = saveReview(9601L, "삭제");
        deleted.delete();
        reviewRepository.save(deleted);
        saveReview(9602L, "다른 상품");

        final var page = reviewRepository.findActiveReviews(new ReviewTarget(ReviewTargetType.PRODUCT, 9601L), new OffsetLimit(3, 2));
        assertThat(page.content()).extracting(item -> item.id()).containsExactly(activeIds.get(2), activeIds.get(1));
        assertThat(page.content()).allSatisfy(item -> {
            assertThat(item.target().type()).isEqualTo(ReviewTargetType.PRODUCT);
            assertThat(item.target().id()).isEqualTo(9601L);
            assertThat(item.isSpoiler()).isTrue();
        });
        assertThat(page.hasNext()).isTrue();

        final var last = reviewRepository.findActiveReviews(new ReviewTarget(ReviewTargetType.PRODUCT, 9601L), new OffsetLimit(5, 2));
        assertThat(last.content()).extracting(item -> item.id()).containsExactly(activeIds.getFirst());
        assertThat(last.hasNext()).isFalse();
        assertThat(reviewRepository.findActiveReviews(new ReviewTarget(ReviewTargetType.PRODUCT, 9601L), new OffsetLimit(6, 2)).content())
            .isEmpty();
    }

    @Test
    void 결제_후_7일_이내에_구매한_상품을_리뷰_대상으로_조회한다() {
        insertBook(9701L);
        insertBook(9702L);
        insertProduct(9801L, 9701L);
        insertProduct(9802L, 9702L);
        final var cutoff = LocalDateTime.of(2026, 10, 12, 9, 0).minusDays(7);
        final var eligible = saveOrderItem(9801L, "기간 경계");
        final var old = saveOrderItem(9801L, "기간 초과");
        final var other = saveOrderItem(9802L, "다른 대상");
        final var deleted = saveOrderItem(9801L, "삭제 주문상품");
        markPaid(eligible, cutoff);
        jdbc.update("UPDATE orders SET created_at = ? WHERE id = (SELECT order_id FROM order_item WHERE id = ?)", cutoff.minusDays(30),
            eligible);
        markPaid(old, cutoff.minusSeconds(1));
        markPaid(other, cutoff);
        markPaid(deleted, cutoff);
        jdbc.update("UPDATE order_item SET deleted_at = ? WHERE id = ?", cutoff, deleted);

        assertThat(reviewOrders.findRecentOrderItemsForProduct(42L, 9801L, OrderStatus.PAID, cutoff)).containsExactly(eligible);
        assertThat(reviewOrders.findRecentOrderItemsForProduct(99L, 9801L, OrderStatus.PAID, cutoff)).isEmpty();
    }

    private void markPaid(final Long orderItemId, final LocalDateTime paidAt) {
        jdbc.update("UPDATE orders SET status = 'PAID', paid_at = ? WHERE id = (SELECT order_id FROM order_item WHERE id = ?)", paidAt,
            orderItemId);
        jdbc.update("UPDATE order_item SET status = 'PAID' WHERE id = ?", orderItemId);
    }

    private Review saveReview(final Long productId, final String content) {
        final Long itemId = saveOrderItem(productId, "review_query_" + content);
        return reviewRepository.save(Review.create(new ReviewKey(42L, "ORDER_ITEM_" + itemId),
            new ReviewTarget(ReviewTargetType.PRODUCT, productId), new ReviewContent(new BigDecimal("4.5"), content, true)));
    }

    private Long saveOrderItem(final Long productId, final String orderNumber) {
        final Order order = Order.create(42L, orderNumber, null);
        order.addItem(productId, "리뷰 테스트 상품", null, "저자", new BigDecimal("20.00"), new BigDecimal("17.25"), 1);
        final var saved = orderRepository.save(order);
        return jdbc.queryForObject("SELECT id FROM order_item WHERE order_id = ?", Long.class, saved.id());
    }

    private void insertBook(final long id) {
        jdbc.update("INSERT INTO books (id, title, author, publisher, category, published_at) VALUES (?, ?, ?, ?, ?, ?)", id, "도서", "저자",
            "출판사", "소설", "2026-01-01");
    }

    private void insertProduct(final long id, final long bookId) {
        jdbc.update(
            "INSERT INTO products (id, book_id, name, sale_price, discounted_price, cost_price, stock_quantity) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?)",
            id, bookId, "상품", new BigDecimal("20.00"), new BigDecimal("17.25"), new BigDecimal("10.00"), 10);
    }
}
