package com.book.core.review.infrastructure.persistence.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.book.core.order.application.port.OrderRepositoryPort;
import com.book.core.order.domain.Order;
import com.book.core.review.application.port.ReviewRepositoryPort;
import com.book.core.review.domain.Review;
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
@ActiveProfiles("test")
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

    @Test
    void review_mapping_soft_delete_and_active_uniqueness_match_the_migration() {
        insertBook(9301L);
        insertProduct(9401L, 9301L);
        final Order order = Order.create(42L, "review_persistence_test", null);
        order.addItem(9401L, "리뷰 테스트 상품", null, "저자", new BigDecimal("20.00"), new BigDecimal("17.25"), 1);
        final Order savedOrder = orderRepository.save(order);
        final Long orderItemId = jdbc.queryForObject("SELECT id FROM order_item WHERE order_id = ?", Long.class, savedOrder.id());
        final Review review = reviewRepository.save(Review.create(42L, orderItemId, new BigDecimal("4.5"), "리뷰 본문", true));

        assertThat(reviewRepository.findActiveById(review.id())).isPresent();
        assertThat(jdbc.queryForMap("SELECT user_id, order_item_id, rating, content, is_spoiler, active_flag FROM reviews WHERE id = ?",
            review.id())).containsEntry("user_id", 42L).containsEntry("order_item_id", orderItemId)
            .containsEntry("rating", new BigDecimal("4.5")).containsEntry("content", "리뷰 본문").containsEntry("is_spoiler", true)
            .containsEntry("active_flag", 1);
        assertThat(reviewRepository.existsActiveByUserIdAndOrderItemId(42L, orderItemId)).isTrue();
        assertThatThrownBy(() -> reviewRepository.save(Review.create(42L, orderItemId, new BigDecimal("5.0"), "중복", false)))
            .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(
            () -> jdbc.update("INSERT INTO reviews (user_id, order_item_id, rating, content, is_spoiler) VALUES (?, ?, ?, ?, ?)", 43L,
                orderItemId, new BigDecimal("10.1"), "범위 초과", false))
            .isInstanceOf(DataAccessException.class).satisfies(exception -> {
                assertThat(exception.getCause()).isInstanceOf(SQLException.class);
                assertThat(((SQLException) exception.getCause()).getErrorCode()).isEqualTo(MYSQL_CHECK_CONSTRAINT_VIOLATED);
            });

        review.delete();
        reviewRepository.save(review);

        assertThat(reviewRepository.findActiveById(review.id())).isEmpty();
        assertThat(jdbc.queryForObject("SELECT active_flag FROM reviews WHERE id = ?", Integer.class, review.id())).isNull();
        assertThat(reviewRepository.existsActiveByUserIdAndOrderItemId(42L, orderItemId)).isFalse();
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
