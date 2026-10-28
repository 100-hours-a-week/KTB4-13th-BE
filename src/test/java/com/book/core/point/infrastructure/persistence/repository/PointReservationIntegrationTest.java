package com.book.core.point.infrastructure.persistence.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.book.core.point.application.usecase.ConfirmPaymentPointsUseCase;
import com.book.core.point.application.usecase.DeductPointUseCase;
import com.book.core.point.application.usecase.EarnPointUseCase;
import com.book.core.point.application.usecase.ReclaimPaymentBonusUseCase;
import com.book.core.point.application.usecase.ReleasePaymentPointsUseCase;
import com.book.core.point.application.usecase.ReservePaymentPointsUseCase;
import com.book.core.point.domain.PointBalance;
import com.book.core.point.domain.enums.PointType;
import com.book.core.review.application.command.RemoveReviewCommand;
import com.book.core.review.application.port.ReviewRepositoryPort;
import com.book.core.review.application.service.ReviewService;
import com.book.core.review.domain.Review;
import com.book.core.review.domain.ReviewContent;
import com.book.core.review.domain.ReviewKey;
import com.book.core.review.domain.ReviewTarget;
import com.book.core.review.domain.enums.ReviewTargetType;
import java.math.BigDecimal;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
@Tag("integration")
class PointReservationIntegrationTest {
    @Autowired
    private PointBalanceJpaRepository balances;
    @Autowired
    private ReservePaymentPointsUseCase reserve;
    @Autowired
    private ReleasePaymentPointsUseCase release;
    @Autowired
    private ConfirmPaymentPointsUseCase confirm;
    @Autowired
    private ReclaimPaymentBonusUseCase reclaim;
    @Autowired
    private EarnPointUseCase earn;
    @Autowired
    private DeductPointUseCase deduct;
    @Autowired
    private JdbcTemplate jdbc;
    @Autowired
    private ReviewService reviewService;
    @Autowired
    private ReviewRepositoryPort reviews;

    @Test
    void 동시_예약은_가용_금액을_초과할_수_없다() throws Exception {
        final long userId = 880001L;
        balances.save(new PointBalance(null, userId, new BigDecimal("1000")));
        final CountDownLatch start = new CountDownLatch(1);
        final AtomicInteger successes = new AtomicInteger();
        try (final var executor = Executors.newFixedThreadPool(2)) {
            final var first = executor.submit(() -> attemptReserve(start, userId, 77101L, successes));
            final var second = executor.submit(() -> attemptReserve(start, userId, 77102L, successes));
            start.countDown();
            first.get(10, TimeUnit.SECONDS);
            second.get(10, TimeUnit.SECONDS);
        }
        assertThat(successes.get()).isEqualTo(1);
        assertThat(balances.findByUserId(userId).orElseThrow().available()).isEqualByComparingTo("0");
        assertThat(jdbc.queryForObject("select count(*) from point_reservation where user_id = ?", Long.class, userId)).isEqualTo(1);
    }

    @Test
    void 예약된_포인트는_보호하고_예약_해제시_미회수액을_상계한다() {
        final long userId = 880002L;
        balances.save(new PointBalance(null, userId, new BigDecimal("1000")));
        reserve.execute(userId, 77201L, new BigDecimal("1000"), 77301L);
        reclaim.execute(userId, 77401L, new BigDecimal("500"));
        reclaim.execute(userId, 77401L, new BigDecimal("500"));
        assertThat(balances.findByUserId(userId).orElseThrow().reserved()).isEqualByComparingTo("1000");
        assertThat(
            jdbc.queryForObject("select remaining_amount from point_recovery where source_type = 'PAYMENT' and source_reference_id = ?",
                BigDecimal.class, 77401L))
            .isEqualByComparingTo("500");

        release.execute(userId, 77201L);
        release.execute(userId, 77201L);
        assertThat(balances.findByUserId(userId).orElseThrow().available()).isEqualByComparingTo("500");
        assertThat(
            jdbc.queryForObject("select remaining_amount from point_recovery where source_type = 'PAYMENT' and source_reference_id = ?",
                BigDecimal.class, 77401L))
            .isEqualByComparingTo("0");
        assertThat(jdbc.queryForObject("select count(*) from point_recovery_settlement", Long.class)).isPositive();
    }

    @Test
    void 신규_적립은_미회수_순서대로_상계하고_중복_적립하지_않는다() {
        final long userId = 880003L;
        balances.save(new PointBalance(null, userId, BigDecimal.ZERO));
        reclaim.execute(userId, 77501L, new BigDecimal("500"));
        reclaim.execute(userId, 77502L, new BigDecimal("300"));
        earn.execute(userId, PointType.REVIEW, 77601L, new BigDecimal("700"));
        earn.execute(userId, PointType.REVIEW, 77601L, new BigDecimal("700"));
        assertThat(balances.findByUserId(userId).orElseThrow().available()).isEqualByComparingTo("0");
        assertThat(
            jdbc.queryForObject("select remaining_amount from point_recovery where source_type = 'PAYMENT' and source_reference_id = ?",
                BigDecimal.class, 77501L))
            .isEqualByComparingTo("0");
        assertThat(
            jdbc.queryForObject("select remaining_amount from point_recovery where source_type = 'PAYMENT' and source_reference_id = ?",
                BigDecimal.class, 77502L))
            .isEqualByComparingTo("100");
        earn.execute(userId, PointType.REVIEW, 77602L, new BigDecimal("300"));
        assertThat(balances.findByUserId(userId).orElseThrow().available()).isEqualByComparingTo("200");
    }

    @Test
    void 예약_확정은_결제_포인트를_한번만_차감한다() {
        final long userId = 880004L;
        balances.save(new PointBalance(null, userId, new BigDecimal("1000")));
        reserve.execute(userId, 77701L, new BigDecimal("700"), 77702L);
        confirm.execute(userId, 77701L);
        confirm.execute(userId, 77701L);
        final PointBalance balance = balances.findByUserId(userId).orElseThrow();
        assertThat(balance.balance()).isEqualByComparingTo("300");
        assertThat(balance.reserved()).isEqualByComparingTo("0");
        assertThat(
            jdbc.queryForObject("select count(*) from point_history where user_id = ? and reference_id = ?", Long.class, userId, 77702L))
            .isEqualTo(1);
    }

    @Test
    void 리뷰와_결제_미회수는_같은_원본번호여도_별도로_기록하고_발생순으로_상계한다() {
        final long userId = 880005L;
        balances.save(new PointBalance(null, userId, new BigDecimal("1000")));
        reserve.execute(userId, 77801L, new BigDecimal("1000"), 77802L);
        reclaim.execute(userId, 77901L, new BigDecimal("500"));
        deduct.execute(userId, PointType.REVIEW, 77901L, new BigDecimal("300"));
        deduct.execute(userId, PointType.REVIEW, 77901L, new BigDecimal("300"));

        assertThat(jdbc.queryForObject("select count(*) from point_recovery where user_id = ? and source_reference_id = ?", Long.class,
            userId, 77901L)).isEqualTo(2);
        assertThat(balances.findByUserId(userId).orElseThrow().reserved()).isEqualByComparingTo("1000");

        earn.execute(userId, PointType.REVIEW, 77902L, new BigDecimal("700"));

        assertThat(
            jdbc.queryForObject("select remaining_amount from point_recovery where source_type = 'PAYMENT' and source_reference_id = ?",
                BigDecimal.class, 77901L))
            .isEqualByComparingTo("0");
        assertThat(
            jdbc.queryForObject("select remaining_amount from point_recovery where source_type = 'REVIEW' and source_reference_id = ?",
                BigDecimal.class, 77901L))
            .isEqualByComparingTo("100");
        assertThat(
            jdbc.queryForObject("select count(*) from point_history where user_id = ? and reference_id = ? and type = 1 and amount = -500",
                Long.class, userId, 77901L))
            .isEqualTo(1);
        assertThat(
            jdbc.queryForObject("select count(*) from point_history where user_id = ? and reference_id = ? and type = 0 and amount = -200",
                Long.class, userId, 77901L))
            .isEqualTo(1);
        assertThat(balances.findByUserId(userId).orElseThrow().reserved()).isEqualByComparingTo("1000");
    }

    @Test
    void 예약으로_가용_포인트가_없어도_리뷰는_삭제되고_회수_부족분이_기록된다() {
        final long userId = 880006L;
        balances.save(new PointBalance(null, userId, new BigDecimal("1000")));
        reserve.execute(userId, 78001L, new BigDecimal("1000"), 78002L);
        final Review review = reviews.save(Review.create(new ReviewKey(userId, "ORDER_ITEM_880006"),
            new ReviewTarget(ReviewTargetType.PRODUCT, 10L), new ReviewContent(new BigDecimal("4.5"), "본문", false)));

        reviewService.removeReview(new RemoveReviewCommand(userId, review.id()));

        assertThat(jdbc.queryForObject("select count(*) from reviews where id = ? and deleted_at is not null", Long.class, review.id()))
            .isEqualTo(1);
        assertThat(
            jdbc.queryForObject("select remaining_amount from point_recovery where source_type = 'REVIEW' and source_reference_id = ?",
                BigDecimal.class, review.id()))
            .isEqualByComparingTo("1000");
        assertThat(balances.findByUserId(userId).orElseThrow().reserved()).isEqualByComparingTo("1000");
    }

    private void attemptReserve(final CountDownLatch start, final long userId, final long attemptId, final AtomicInteger successes) {
        try {
            start.await();
            reserve.execute(userId, attemptId, new BigDecimal("1000"), attemptId);
            successes.incrementAndGet();
        } catch (final com.book.common.exception.CoreException exception) {
            assertThat(exception.errorCode()).isEqualTo(com.book.common.exception.ErrorCode.POINT_EXCEEDS_BALANCE);
        } catch (final InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(exception);
        }
    }
}
