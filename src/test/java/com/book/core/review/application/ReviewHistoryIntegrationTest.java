package com.book.core.review.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.book.core.review.application.port.ReviewRepositoryPort;
import com.book.core.review.application.service.ReviewService;
import com.book.core.review.application.command.AddReviewCommand;
import com.book.core.review.application.command.UpdateReviewCommand;
import com.book.core.review.application.command.RemoveReviewCommand;
import com.book.core.review.application.usecase.ValidateReviewPolicyUseCase;
import com.book.core.point.application.usecase.EarnPointUseCase;
import com.book.core.point.application.usecase.DeductPointUseCase;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import com.book.core.review.domain.Review;
import com.book.core.review.domain.ReviewContent;
import com.book.core.review.domain.ReviewKey;
import com.book.core.review.domain.ReviewTarget;
import com.book.core.review.domain.enums.ReviewTargetType;
import java.math.BigDecimal;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;

@SpringBootTest
@ActiveProfiles({"test", "mysql"})
@Testcontainers
@Tag("integration")
class ReviewHistoryIntegrationTest {
    @Container
    @ServiceConnection
    static final MySQLContainer mysql = new MySQLContainer("mysql:8.4.8");

    @Autowired
    ReviewService service;

    @MockitoBean
    ValidateReviewPolicyUseCase policy;

    @MockitoBean
    EarnPointUseCase earn;

    @MockitoBean
    DeductPointUseCase deduct;

    @Autowired
    ReviewRepositoryPort reviews;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    PlatformTransactionManager transactionManager;

    private final ReviewTarget target = new ReviewTarget(ReviewTargetType.PRODUCT, 10L);

    @Test
    void 리뷰_생성부터_삭제까지_모든_변경_상태를_최신순으로_추적할_수_있다() {
        final Long id = addReview(target, new ReviewKey(42L, "history-flow"), content("4.5", "최초 본문", false));
        updateReview(42L, id, content("4.50", "최초 본문", null));
        updateReview(42L, id, content("4.5", "최초 본문", true));
        updateReview(42L, id, content("1.0", "수정 본문", null));
        removeReview(42L, id);

        final var rows = jdbc.queryForList("SELECT * FROM review_history WHERE review_id = ? ORDER BY id DESC", id);
        assertThat(rows).extracting((final var row) -> row.get("event_type")).containsExactly("DELETE", "UPDATE", "UPDATE", "UPDATE",
            "CREATE");
        assertThat(rows).extracting((final var row) -> row.get("content")).containsExactly("수정 본문", "수정 본문", "최초 본문", "최초 본문", "최초 본문");
        assertThat(rows).extracting((final var row) -> ((BigDecimal) row.get("rate")).toPlainString()).containsExactly("1.0", "1.0", "4.5",
            "4.5", "4.5");
        assertThat(reviews.findActiveById(id)).isEmpty();
    }

    @Test
    void 기존_리뷰에는_과거_이력을_만들지_않고_첫_변경부터_기록한다() {
        final Review review = existingReview("legacy-review");
        assertThat(historyCount(review.id())).isZero();

        updateReview(42L, review.id(), content("3.0", "도입 후 수정", null));

        assertThat(jdbc.queryForList("SELECT event_type FROM review_history WHERE review_id = ?", String.class, review.id()))
            .containsExactly("UPDATE");
        assertThat(jdbc.queryForObject("SELECT content FROM review_history WHERE review_id = ?", String.class, review.id()))
            .isEqualTo("도입 후 수정");
    }

    @Test
    void 이력_저장에_실패하면_리뷰_생성_수정_삭제를_모두_롤백한다() {
        final ReviewTarget failureTarget = new ReviewTarget(ReviewTargetType.PRODUCT, 999292L);
        final Review review =
            reviews.save(Review.create(new ReviewKey(42L, "history-rollback-existing"), failureTarget, content("4.5", "기존 본문", false)));
        jdbc.execute("ALTER TABLE review_history ADD CONSTRAINT chk_synthetic_history_failure CHECK (target_id <> 999292)");
        try {
            assertThatThrownBy(
                () -> addReview(failureTarget, new ReviewKey(42L, "history-rollback-create"), content("4.5", "생성 실패", false)))
                .isInstanceOf(DataAccessException.class);
            assertThatThrownBy(() -> updateReview(42L, review.id(), content("1.0", "수정 실패", true))).isInstanceOf(DataAccessException.class);
            assertThatThrownBy(() -> removeReview(42L, review.id())).isInstanceOf(DataAccessException.class);
        } finally {
            jdbc.execute("ALTER TABLE review_history DROP CHECK chk_synthetic_history_failure");
        }

        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM reviews WHERE review_key = 'history-rollback-create'", Integer.class))
            .isZero();
        final Review unchanged = reviews.findActiveById(review.id()).orElseThrow();
        assertThat(unchanged.content()).isEqualTo("기존 본문");
        assertThat(unchanged.rate()).isEqualByComparingTo("4.5");
        assertThat(unchanged.isSpoiler()).isFalse();
        assertThat(historyCount(review.id())).isZero();
    }

    @Test
    void 같은_리뷰의_수정이_끝날_때까지_삭제는_대기하고_다른_리뷰는_수정할_수_있다() throws Exception {
        final Review first = existingReview("history-lock-first");
        final Review other = existingReview("history-lock-other");
        final var written = new CountDownLatch(1);
        final var release = new CountDownLatch(1);
        final var transaction = new TransactionTemplate(transactionManager);
        try (final var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            final var firstWrite = executor.submit(() -> transaction.executeWithoutResult((final var status) -> {
                updateReview(42L, first.id(), content("2.0", "잠금 안의 수정", null));
                written.countDown();
                await(release);
            }));
            try {
                assertThat(written.await(10, TimeUnit.SECONDS)).isTrue();
                // 별도 연결에서 1초 내 잠금 획득 실패를 확인합니다. 세션 설정은 반드시 복원합니다.
                assertThatThrownBy(() -> transaction.executeWithoutResult((final var status) -> {
                    jdbc.execute("SET SESSION innodb_lock_wait_timeout = 1");
                    try {
                        removeReview(42L, first.id());
                    } finally {
                        jdbc.execute("SET SESSION innodb_lock_wait_timeout = DEFAULT");
                    }
                })).isInstanceOf(DataAccessException.class);

                final var independentWrite = executor.submit(() -> updateReview(42L, other.id(), content("3.0", "독립 수정", null)));
                assertThat(independentWrite.get(5, TimeUnit.SECONDS)).isEqualTo(other.id());
            } finally {
                release.countDown();
            }
            firstWrite.get(10, TimeUnit.SECONDS);
        }
        removeReview(42L, first.id());

        final var rows =
            jdbc.queryForList("SELECT event_type, content FROM review_history WHERE review_id = ? ORDER BY id DESC", first.id());
        assertThat(rows).extracting((final var row) -> row.get("event_type")).containsExactly("DELETE", "UPDATE");
        assertThat(rows).extracting((final var row) -> row.get("content")).containsExactly("잠금 안의 수정", "잠금 안의 수정");
        assertThat(reviews.findActiveById(first.id())).isEmpty();
    }

    @Test
    void 리뷰_삭제는_수정_커밋까지_대기한_뒤_최신_본문을_이력에_남긴다() throws Exception {
        final Review review = existingReview("history-concurrent-success");
        final var written = new CountDownLatch(1);
        final var deleteStarted = new CountDownLatch(1);
        final var release = new CountDownLatch(1);
        final var transaction = new TransactionTemplate(transactionManager);
        try (final var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            final var firstWrite = executor.submit(() -> transaction.executeWithoutResult((final var status) -> {
                updateReview(42L, review.id(), content("2.0", "삭제 전에 수정된 본문", null));
                written.countDown();
                await(release);
            }));
            try {
                assertThat(written.await(10, TimeUnit.SECONDS)).isTrue();
                final var waitingDelete = executor.submit(() -> {
                    deleteStarted.countDown();
                    return removeReview(42L, review.id());
                });
                assertThat(deleteStarted.await(10, TimeUnit.SECONDS)).isTrue();
                assertThatThrownBy(() -> waitingDelete.get(300, TimeUnit.MILLISECONDS)).isInstanceOf(TimeoutException.class);
                release.countDown();
                firstWrite.get(10, TimeUnit.SECONDS);
                assertThat(waitingDelete.get(10, TimeUnit.SECONDS)).isEqualTo(review.id());
            } finally {
                release.countDown();
            }
        }

        final var rows =
            jdbc.queryForList("SELECT id, event_type, content FROM review_history WHERE review_id = ? ORDER BY id DESC", review.id());
        assertThat(rows).extracting((final var row) -> row.get("event_type")).containsExactly("DELETE", "UPDATE");
        assertThat(rows).extracting((final var row) -> row.get("content")).containsExactly("삭제 전에 수정된 본문", "삭제 전에 수정된 본문");
        assertThat((Long) rows.getFirst().get("id")).isGreaterThan((Long) rows.getLast().get("id"));
        assertThat(reviews.findActiveById(review.id())).isEmpty();
    }

    private Long addReview(final ReviewTarget target, final ReviewKey key, final ReviewContent content) {
        when(policy.validateNew(key.userId(), target)).thenReturn(key);
        return service.addReview(new AddReviewCommand(key.userId(), target, content));
    }

    private Long updateReview(final Long userId, final Long reviewId, final ReviewContent content) {
        return service.updateReview(new UpdateReviewCommand(userId, reviewId, content));
    }

    private Long removeReview(final Long userId, final Long reviewId) {
        return service.removeReview(new RemoveReviewCommand(userId, reviewId));
    }

    private Review existingReview(final String key) {
        return reviews.save(Review.create(new ReviewKey(42L, key), target, content("4.5", "기존 본문", false)));
    }

    private int historyCount(final Long reviewId) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM review_history WHERE review_id = ?", Integer.class, reviewId);
    }

    private ReviewContent content(final String rate, final String text, final Boolean isSpoiler) {
        return new ReviewContent(new BigDecimal(rate), text, isSpoiler);
    }

    private void await(final CountDownLatch latch) {
        try {
            if (!latch.await(15, TimeUnit.SECONDS)) {
                throw new IllegalStateException("test coordination timed out");
            }
        } catch (final InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(exception);
        }
    }
}
