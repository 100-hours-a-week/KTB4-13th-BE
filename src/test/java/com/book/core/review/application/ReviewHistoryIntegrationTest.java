package com.book.core.review.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.book.core.review.application.port.ReviewRepositoryPort;
import com.book.core.review.application.usecase.AddReviewUseCase;
import com.book.core.review.application.usecase.DeleteReviewUseCase;
import com.book.core.review.application.usecase.UpdateReviewUseCase;
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
@ActiveProfiles("test")
@Testcontainers
@Tag("integration")
class ReviewHistoryIntegrationTest {
    @Container
    @ServiceConnection
    static final MySQLContainer mysql = new MySQLContainer("mysql:8.4.8");

    @Autowired
    AddReviewUseCase add;

    @Autowired
    UpdateReviewUseCase update;

    @Autowired
    DeleteReviewUseCase delete;

    @Autowired
    ReviewRepositoryPort reviews;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    PlatformTransactionManager transactionManager;

    private final ReviewTarget target = new ReviewTarget(ReviewTargetType.PRODUCT, 10L);

    @Test
    void 생성_수정_삭제를_역순으로_추적하고_동일_수정은_제외한다() {
        final Long id = add.execute(target, new ReviewKey(42L, "history-flow"), content("4.5", "최초 본문"), false).id();
        update.execute(42L, id, content("4.50", "최초 본문"), null);
        update.execute(42L, id, content("4.5", "최초 본문"), true);
        update.execute(42L, id, content("1.0", "수정 본문"), null);
        delete.execute(42L, id);

        final var rows = jdbc.queryForList("SELECT * FROM review_history WHERE review_id = ? ORDER BY changed_at DESC, id DESC", id);
        assertThat(rows).extracting((final var row) -> row.get("operation")).containsExactly("DELETE", "UPDATE", "UPDATE", "CREATE");
        assertThat(rows).extracting((final var row) -> row.get("content")).containsExactly("수정 본문", "수정 본문", "최초 본문", "최초 본문");
        assertThat(rows).extracting((final var row) -> ((BigDecimal) row.get("rate")).toPlainString()).containsExactly("1.0", "1.0", "4.5",
            "4.5");
        assertThat(reviews.findActiveById(id)).isEmpty();
    }

    @Test
    void 기존_리뷰는_초기_이력_없이_도입_후_변경부터_기록한다() {
        final Review review = existingReview("legacy-review");
        assertThat(historyCount(review.id())).isZero();

        update.execute(42L, review.id(), content("3.0", "도입 후 수정"), null);

        assertThat(jdbc.queryForList("SELECT operation FROM review_history WHERE review_id = ?", String.class, review.id()))
            .containsExactly("UPDATE");
        assertThat(jdbc.queryForObject("SELECT content FROM review_history WHERE review_id = ?", String.class, review.id()))
            .isEqualTo("도입 후 수정");
    }

    @Test
    void 실제_DB_이력_저장_실패는_생성_수정_삭제를_모두_롤백한다() {
        final ReviewTarget failureTarget = new ReviewTarget(ReviewTargetType.PRODUCT, 999292L);
        final Review review =
            reviews.save(Review.create(new ReviewKey(42L, "history-rollback-existing"), failureTarget, content("4.5", "기존 본문"), false));
        jdbc.execute("ALTER TABLE review_history ADD CONSTRAINT chk_synthetic_history_failure CHECK (target_id <> 999292)");
        try {
            assertThatThrownBy(
                () -> add.execute(failureTarget, new ReviewKey(42L, "history-rollback-create"), content("4.5", "생성 실패"), false))
                .isInstanceOf(DataAccessException.class);
            assertThatThrownBy(() -> update.execute(42L, review.id(), content("1.0", "수정 실패"), true))
                .isInstanceOf(DataAccessException.class);
            assertThatThrownBy(() -> delete.execute(42L, review.id())).isInstanceOf(DataAccessException.class);
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
    void 같은_리뷰의_수정_중에는_삭제가_대기하고_다른_리뷰는_변경할_수_있다() throws Exception {
        final Review first = existingReview("history-lock-first");
        final Review other = existingReview("history-lock-other");
        final var written = new CountDownLatch(1);
        final var release = new CountDownLatch(1);
        final var transaction = new TransactionTemplate(transactionManager);
        try (final var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            final var firstWrite = executor.submit(() -> transaction.executeWithoutResult((final var status) -> {
                update.execute(42L, first.id(), content("2.0", "잠금 안의 수정"), null);
                written.countDown();
                await(release);
            }));
            try {
                assertThat(written.await(10, TimeUnit.SECONDS)).isTrue();
                // 별도 연결에서 1초 내 잠금 획득 실패를 확인합니다. 세션 설정은 반드시 복원합니다.
                assertThatThrownBy(() -> transaction.executeWithoutResult((final var status) -> {
                    jdbc.execute("SET SESSION innodb_lock_wait_timeout = 1");
                    try {
                        delete.execute(42L, first.id());
                    } finally {
                        jdbc.execute("SET SESSION innodb_lock_wait_timeout = DEFAULT");
                    }
                })).isInstanceOf(DataAccessException.class);

                final var independentWrite = executor.submit(() -> update.execute(42L, other.id(), content("3.0", "독립 수정"), null));
                assertThat(independentWrite.get(5, TimeUnit.SECONDS)).isEqualTo(other.id());
            } finally {
                release.countDown();
            }
            firstWrite.get(10, TimeUnit.SECONDS);
        }
        delete.execute(42L, first.id());

        final var rows = jdbc.queryForList(
            "SELECT operation, content FROM review_history WHERE review_id = ? " + "ORDER BY changed_at DESC, id DESC", first.id());
        assertThat(rows).extracting((final var row) -> row.get("operation")).containsExactly("DELETE", "UPDATE");
        assertThat(rows).extracting((final var row) -> row.get("content")).containsExactly("잠금 안의 수정", "잠금 안의 수정");
        assertThat(reviews.findActiveById(first.id())).isEmpty();
    }

    @Test
    void 대기하던_삭제가_수정_커밋_후_성공하고_수정된_본문을_이력에_남긴다() throws Exception {
        final Review review = existingReview("history-concurrent-success");
        final var written = new CountDownLatch(1);
        final var deleteStarted = new CountDownLatch(1);
        final var release = new CountDownLatch(1);
        final var transaction = new TransactionTemplate(transactionManager);
        try (final var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            final var firstWrite = executor.submit(() -> transaction.executeWithoutResult((final var status) -> {
                update.execute(42L, review.id(), content("2.0", "삭제 전에 수정된 본문"), null);
                written.countDown();
                await(release);
            }));
            try {
                assertThat(written.await(10, TimeUnit.SECONDS)).isTrue();
                final var waitingDelete = executor.submit(() -> {
                    deleteStarted.countDown();
                    return delete.execute(42L, review.id());
                });
                assertThat(deleteStarted.await(10, TimeUnit.SECONDS)).isTrue();
                assertThatThrownBy(() -> waitingDelete.get(300, TimeUnit.MILLISECONDS)).isInstanceOf(TimeoutException.class);
                release.countDown();
                firstWrite.get(10, TimeUnit.SECONDS);
                assertThat(waitingDelete.get(10, TimeUnit.SECONDS).id()).isEqualTo(review.id());
            } finally {
                release.countDown();
            }
        }

        final var rows = jdbc.queryForList(
            "SELECT id, operation, content FROM review_history WHERE review_id = ? " + "ORDER BY changed_at DESC, id DESC", review.id());
        assertThat(rows).extracting((final var row) -> row.get("operation")).containsExactly("DELETE", "UPDATE");
        assertThat(rows).extracting((final var row) -> row.get("content")).containsExactly("삭제 전에 수정된 본문", "삭제 전에 수정된 본문");
        assertThat((Long) rows.getFirst().get("id")).isGreaterThan((Long) rows.getLast().get("id"));
        assertThat(reviews.findActiveById(review.id())).isEmpty();
    }

    private Review existingReview(final String key) {
        return reviews.save(Review.create(new ReviewKey(42L, key), target, content("4.5", "기존 본문"), false));
    }

    private int historyCount(final Long reviewId) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM review_history WHERE review_id = ?", Integer.class, reviewId);
    }

    private ReviewContent content(final String rate, final String text) {
        return new ReviewContent(new BigDecimal(rate), text);
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
