package com.book.core.review.infrastructure.persistence.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.book.core.review.application.port.ReviewHistoryRepositoryPort;
import com.book.core.review.application.port.ReviewRepositoryPort;
import com.book.core.review.domain.Review;
import com.book.core.review.domain.ReviewContent;
import com.book.core.review.domain.ReviewHistory;
import com.book.core.review.domain.ReviewKey;
import com.book.core.review.domain.ReviewTarget;
import com.book.core.review.domain.enums.ReviewOperation;
import com.book.core.review.domain.enums.ReviewTargetType;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
@Tag("integration")
class ReviewHistoryRepositoryIntegrationTest {
    @Container
    @ServiceConnection
    static final MySQLContainer mysql = new MySQLContainer("mysql:8.4.8");

    @Autowired
    ReviewRepositoryPort reviews;

    @Autowired
    ReviewHistoryRepositoryPort histories;

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void 원본_수정과_삭제_후에도_전체_상태와_마이크로초_시각을_보존한다() {
        final var review = reviews.save(Review.create(new ReviewKey(42L, "history-mapping"),
            new ReviewTarget(ReviewTargetType.PRODUCT, 10L), new ReviewContent(new BigDecimal("4.5"), "최초 본문"), false));
        final var changedAt = LocalDateTime.of(2026, 10, 14, 12, 0, 0, 123456000);
        histories.append(new ReviewHistory(review, ReviewOperation.CREATE, changedAt, "2001:db8::1"));
        review.updateContent(new ReviewContent(new BigDecimal("1.0"), "수정 본문"), true);
        review.delete();
        reviews.save(review);

        final var row = jdbc.queryForMap("SELECT * FROM review_history WHERE review_id = ?", review.id());
        assertThat(row).containsEntry("user_id", 42L).containsEntry("target_type", "PRODUCT").containsEntry("target_id", 10L)
            .containsEntry("operation", "CREATE").containsEntry("content", "최초 본문").containsEntry("request_ip", "2001:db8::1");
        assertThat((BigDecimal) row.get("rate")).isEqualByComparingTo("4.5");
        assertThat(jdbc.queryForObject("SELECT changed_at FROM review_history WHERE review_id = ?", LocalDateTime.class, review.id()))
            .isEqualTo(changedAt);
        assertThat(row).doesNotContainKeys("is_spoiler", "version", "deleted_at", "updated_at");
    }
}
