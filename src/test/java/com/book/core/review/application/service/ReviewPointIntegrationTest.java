package com.book.core.review.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import com.book.core.review.application.command.AddReviewCommand;
import com.book.core.review.application.command.RemoveReviewCommand;
import com.book.core.review.application.port.ReviewRepositoryPort;
import com.book.core.review.application.usecase.ValidateReviewPolicyUseCase;
import com.book.core.review.domain.Review;
import com.book.core.review.domain.ReviewContent;
import com.book.core.review.domain.ReviewKey;
import com.book.core.review.domain.ReviewTarget;
import com.book.core.review.domain.enums.ReviewTargetType;
import java.math.BigDecimal;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;

@SpringBootTest
@ActiveProfiles({"test", "mysql"})
@Testcontainers
@Tag("integration")
class ReviewPointIntegrationTest {
    @Container
    @ServiceConnection
    static final MySQLContainer mysql = new MySQLContainer("mysql:8.4.8");

    @Autowired
    ReviewService service;

    @Autowired
    ReviewRepositoryPort reviews;

    @Autowired
    JdbcTemplate jdbc;

    @MockitoBean
    ValidateReviewPolicyUseCase policy;

    @Test
    void 포인트_적립이_실패하면_리뷰와_이력을_함께_롤백한다() {
        final Long userId = 987601L;
        final var target = new ReviewTarget(ReviewTargetType.PRODUCT, 10L);
        final var key = new ReviewKey(userId, "ORDER_ITEM_987601");
        when(policy.validateNew(userId, target)).thenReturn(key);

        assertThatThrownBy(
            () -> service.addReview(new AddReviewCommand(userId, target, new ReviewContent(new BigDecimal("4.5"), "본문", false))))
            .isInstanceOfSatisfying(CoreException.class,
                (final var exception) -> assertThat(exception.errorCode()).isEqualTo(ErrorCode.NOT_FOUND_DATA));

        assertThat(
            jdbc.queryForObject("SELECT COUNT(*) FROM reviews WHERE user_id = ? AND review_key = ?", Integer.class, userId, key.key()))
            .isZero();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM point_history WHERE user_id = ?", Integer.class, userId)).isZero();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM review_history WHERE user_id = ?", Integer.class, userId)).isZero();
    }

    @Test
    void 포인트_차감이_실패하면_리뷰_삭제와_이력을_함께_롤백한다() {
        final Long userId = 987602L;
        final Review review = reviews.save(Review.create(new ReviewKey(userId, "ORDER_ITEM_987602"),
            new ReviewTarget(ReviewTargetType.PRODUCT, 10L), new ReviewContent(new BigDecimal("4.5"), "본문", false)));

        assertThatThrownBy(() -> service.removeReview(new RemoveReviewCommand(userId, review.id()))).isInstanceOfSatisfying(
            CoreException.class, (final var exception) -> assertThat(exception.errorCode()).isEqualTo(ErrorCode.NOT_FOUND_DATA));

        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM reviews WHERE id = ? AND deleted_at IS NULL", Integer.class, review.id()))
            .isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM point_history WHERE user_id = ?", Integer.class, userId)).isZero();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM review_history WHERE user_id = ?", Integer.class, userId)).isZero();
    }
}
