package com.book.core.review.infrastructure.persistence.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.book.core.review.application.port.ReviewHistoryRepositoryPort;
import com.book.core.review.application.port.ReviewRepositoryPort;
import com.book.core.review.domain.Review;
import com.book.core.review.domain.ReviewContent;
import com.book.core.review.domain.ReviewHistory;
import com.book.core.review.domain.ReviewKey;
import com.book.core.review.domain.ReviewTarget;
import com.book.core.review.domain.enums.ReviewEventType;
import com.book.core.review.domain.enums.ReviewTargetType;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
@Tag("integration")
class ReviewHistoryRepositoryIntegrationTest {
    @Autowired
    ReviewRepositoryPort reviews;

    @Autowired
    ReviewHistoryRepositoryPort histories;

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void 리뷰를_수정하거나_삭제해도_이력은_저장_당시_내용과_생성_시각을_보존한다() {
        final var review = reviews.save(Review.create(new ReviewKey(42L, "history-mapping"),
            new ReviewTarget(ReviewTargetType.PRODUCT, 10L), new ReviewContent(new BigDecimal("4.5"), "최초 본문", false)));
        histories.append(new ReviewHistory(review, ReviewEventType.CREATE));
        review.updateContent(new ReviewContent(new BigDecimal("1.0"), "수정 본문", true));
        review.delete();
        reviews.save(review);

        final var row = jdbc.queryForMap("SELECT * FROM review_history WHERE review_id = ?", review.id());
        assertThat(row).containsEntry("user_id", 42L).containsEntry("target_type", "PRODUCT").containsEntry("target_id", 10L)
            .containsEntry("event_type", "CREATE").containsEntry("content", "최초 본문");
        assertThat((BigDecimal) row.get("rate")).isEqualByComparingTo("4.5");
        final var createdAt =
            jdbc.queryForObject("SELECT created_at FROM review_history WHERE review_id = ?", LocalDateTime.class, review.id());
        assertThat(createdAt).isNotNull();
        assertThat(createdAt.getNano() % 1000).isZero();
        assertThat(row).containsKeys("created_at", "updated_at", "deleted_at");
        assertThat(row.get("created_at")).isNotNull();
        assertThat(row.get("updated_at")).isNotNull();
        assertThat(row).doesNotContainKeys("changed_at", "is_spoiler", "version", "request_ip");
    }
}
