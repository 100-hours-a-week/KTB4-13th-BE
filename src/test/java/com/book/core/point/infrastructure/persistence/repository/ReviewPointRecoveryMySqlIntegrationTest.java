package com.book.core.point.infrastructure.persistence.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.book.core.point.application.usecase.DeductPointUseCase;
import com.book.core.point.application.usecase.EarnPointUseCase;
import com.book.core.point.domain.PointBalance;
import com.book.core.point.domain.enums.PointType;
import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;

@SpringBootTest
@ActiveProfiles({"test", "mysql-test"})
@Testcontainers
@Tag("integration")
@Tag("mysql-migration")
class ReviewPointRecoveryMySqlIntegrationTest {
    @Container
    @ServiceConnection
    static final MySQLContainer mysql = new MySQLContainer("mysql:8.4.8");

    @Autowired
    private PointBalanceJpaRepository balances;
    @Autowired
    private EarnPointUseCase earn;
    @Autowired
    private DeductPointUseCase deduct;
    @Autowired
    private JdbcTemplate jdbc;
    @Autowired
    private PlatformTransactionManager transactions;

    @Test
    void 리뷰_보너스를_회수할_잔액이_충분하면_한번만_차감한다() {
        final long userId = 8_121_001L;
        final long reviewId = 8_122_001L;
        balances.save(new PointBalance(null, userId, new BigDecimal("800")));

        deduct.execute(userId, PointType.REVIEW, reviewId, new BigDecimal("500"));
        deduct.execute(userId, PointType.REVIEW, reviewId, new BigDecimal("500"));

        assertThat(balance(userId)).isEqualByComparingTo("300");
        assertThat(remaining(userId)).usingElementComparator(BigDecimal::compareTo).containsExactly(new BigDecimal("0"));
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM point_history WHERE user_id = ?", Long.class, userId)).isEqualTo(1L);
    }

    @Test
    void 리뷰_보너스의_부족분은_기록하고_새로_적립하면_오래된_건부터_상계한다() {
        final long userId = 8_121_002L;
        balances.save(new PointBalance(null, userId, new BigDecimal("100")));

        deduct.execute(userId, PointType.REVIEW, 8_122_002L, new BigDecimal("500"));
        deduct.execute(userId, PointType.REVIEW, 8_122_003L, new BigDecimal("300"));
        assertThat(balance(userId)).isEqualByComparingTo("0");
        assertThat(remaining(userId)).usingElementComparator(BigDecimal::compareTo).containsExactly(new BigDecimal("400"),
            new BigDecimal("300"));

        earn.execute(userId, PointType.REVIEW, 8_122_004L, new BigDecimal("600"));

        assertThat(balance(userId)).isEqualByComparingTo("0");
        assertThat(remaining(userId)).usingElementComparator(BigDecimal::compareTo).containsExactly(new BigDecimal("0"),
            new BigDecimal("100"));
        earn.execute(userId, PointType.REVIEW, 8_122_005L, new BigDecimal("200"));
        assertThat(balance(userId)).isEqualByComparingTo("100");
        assertThat(remaining(userId)).usingElementComparator(BigDecimal::compareTo).containsExactly(new BigDecimal("0"),
            new BigDecimal("0"));
    }

    @Test
    void 같은_리뷰_회수를_동시에_요청해도_한번만_반영한다() throws Exception {
        final long userId = 8_121_003L;
        final long reviewId = 8_122_006L;
        balances.save(new PointBalance(null, userId, new BigDecimal("100")));
        final CountDownLatch ready = new CountDownLatch(2);
        final CountDownLatch start = new CountDownLatch(1);
        try (final var executor = Executors.newFixedThreadPool(2)) {
            final var first = executor.submit(() -> {
                reclaimAfterStart(userId, reviewId, ready, start);
                return null;
            });
            final var second = executor.submit(() -> {
                reclaimAfterStart(userId, reviewId, ready, start);
                return null;
            });
            assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            first.get(20, TimeUnit.SECONDS);
            second.get(20, TimeUnit.SECONDS);
        }
        assertThat(balance(userId)).isEqualByComparingTo("0");
        assertThat(remaining(userId)).usingElementComparator(BigDecimal::compareTo).containsExactly(new BigDecimal("400"));
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM point_history WHERE user_id = ?", Long.class, userId)).isEqualTo(1L);
    }

    @Test
    void 서로_다른_적립을_동시에_상계해도_미회수액을_중복_회수하지_않는다() throws Exception {
        final long userId = 8_121_005L;
        balances.save(new PointBalance(null, userId, BigDecimal.ZERO));
        deduct.execute(userId, PointType.REVIEW, 8_122_009L, new BigDecimal("700"));
        final CountDownLatch ready = new CountDownLatch(2);
        final CountDownLatch start = new CountDownLatch(1);
        try (final var executor = Executors.newFixedThreadPool(2)) {
            final var first = executor.submit(() -> {
                earnAfterStart(userId, 8_122_010L, "200", ready, start);
                return null;
            });
            final var second = executor.submit(() -> {
                earnAfterStart(userId, 8_122_011L, "300", ready, start);
                return null;
            });
            assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            first.get(20, TimeUnit.SECONDS);
            second.get(20, TimeUnit.SECONDS);
        }
        assertThat(balance(userId)).isEqualByComparingTo("0");
        assertThat(remaining(userId)).usingElementComparator(BigDecimal::compareTo).containsExactly(new BigDecimal("200"));
    }

    @Test
    void 같은_결제의_부분취소_포인트가_여러번_복원되면_각각_상계한다() {
        final long userId = 8_121_006L;
        final long paymentId = 8_123_001L;
        balances.save(new PointBalance(null, userId, BigDecimal.ZERO));
        deduct.execute(userId, PointType.REVIEW, 8_122_012L, new BigDecimal("700"));

        earn.execute(userId, PointType.PAYMENT, paymentId, new BigDecimal("300"));
        earn.execute(userId, PointType.PAYMENT, paymentId, new BigDecimal("300"));
        assertThat(balance(userId)).isEqualByComparingTo("0");
        assertThat(remaining(userId)).usingElementComparator(BigDecimal::compareTo).containsExactly(new BigDecimal("100"));
        earn.execute(userId, PointType.PAYMENT, paymentId, new BigDecimal("150"));
        assertThat(balance(userId)).isEqualByComparingTo("50");
        assertThat(remaining(userId)).usingElementComparator(BigDecimal::compareTo).containsExactly(new BigDecimal("0"));
        assertThat(
            jdbc.queryForList("SELECT amount FROM point_recovery_settlement WHERE source_type = 'PAYMENT' ORDER BY id", BigDecimal.class))
            .usingElementComparator(BigDecimal::compareTo)
            .containsExactly(new BigDecimal("300"), new BigDecimal("300"), new BigDecimal("100"));
    }

    @Test
    void 이전_읽기_스냅샷이_있어도_이후_생성된_미회수에_적립을_상계한다() throws Exception {
        final long userId = 8_121_007L;
        balances.save(new PointBalance(null, userId, new BigDecimal("100")));
        final CountDownLatch snapshotRead = new CountDownLatch(1);
        final CountDownLatch continueTransaction = new CountDownLatch(1);
        try (final var executor = Executors.newSingleThreadExecutor()) {
            final var earning = executor.submit(() -> new TransactionTemplate(transactions).executeWithoutResult(status -> {
                jdbc.queryForObject("SELECT COUNT(*) FROM point_recovery WHERE user_id = ?", Long.class, userId);
                snapshotRead.countDown();
                await(continueTransaction);
                earn.execute(userId, PointType.REVIEW, 8_122_014L, new BigDecimal("400"));
            }));
            assertThat(snapshotRead.await(10, TimeUnit.SECONDS)).isTrue();
            deduct.execute(userId, PointType.REVIEW, 8_122_013L, new BigDecimal("500"));
            continueTransaction.countDown();
            earning.get(20, TimeUnit.SECONDS);
        }
        assertThat(balance(userId)).isEqualByComparingTo("0");
        assertThat(remaining(userId)).usingElementComparator(BigDecimal::compareTo).containsExactly(new BigDecimal("0"));
    }

    @Test
    void 이전_읽기_스냅샷이_있어도_다른_트랜잭션의_상계_결과를_반영한다() throws Exception {
        final long userId = 8_121_008L;
        balances.save(new PointBalance(null, userId, BigDecimal.ZERO));
        deduct.execute(userId, PointType.REVIEW, 8_122_015L, new BigDecimal("500"));
        final CountDownLatch snapshotRead = new CountDownLatch(1);
        final CountDownLatch continueTransaction = new CountDownLatch(1);
        try (final var executor = Executors.newSingleThreadExecutor()) {
            final var earning = executor.submit(() -> new TransactionTemplate(transactions).executeWithoutResult(status -> {
                jdbc.queryForObject("SELECT COUNT(*) FROM point_recovery WHERE user_id = ?", Long.class, userId);
                snapshotRead.countDown();
                await(continueTransaction);
                earn.execute(userId, PointType.REVIEW, 8_122_017L, new BigDecimal("300"));
            }));
            assertThat(snapshotRead.await(10, TimeUnit.SECONDS)).isTrue();
            earn.execute(userId, PointType.REVIEW, 8_122_016L, new BigDecimal("200"));
            continueTransaction.countDown();
            earning.get(20, TimeUnit.SECONDS);
        }
        assertThat(balance(userId)).isEqualByComparingTo("0");
        assertThat(remaining(userId)).usingElementComparator(BigDecimal::compareTo).containsExactly(new BigDecimal("0"));
    }

    @Test
    void 소수_취소_복원액은_DB에_실제로_적용될_금액을_기준으로_상계한다() {
        final long userId = 8_121_009L;
        balances.save(new PointBalance(null, userId, BigDecimal.ZERO));
        deduct.execute(userId, PointType.REVIEW, 8_122_018L, new BigDecimal("2"));

        earn.execute(userId, PointType.PAYMENT, 8_123_002L, new BigDecimal("1.5"));

        assertThat(balance(userId)).isEqualByComparingTo("0");
        assertThat(remaining(userId)).usingElementComparator(BigDecimal::compareTo).containsExactly(new BigDecimal("0"));
    }

    @Test
    void 기존_음수_잔액은_새로운_미회수_건에_포함하지_않는다() {
        final long userId = 8_121_004L;
        balances.save(new PointBalance(null, userId, new BigDecimal("-3000")));

        deduct.execute(userId, PointType.REVIEW, 8_122_007L, new BigDecimal("200"));

        assertThat(balance(userId)).isEqualByComparingTo("-3000");
        assertThat(remaining(userId)).usingElementComparator(BigDecimal::compareTo).containsExactly(new BigDecimal("200"));
        earn.execute(userId, PointType.REVIEW, 8_122_008L, new BigDecimal("1000"));
        assertThat(balance(userId)).isEqualByComparingTo("-2000");
        assertThat(remaining(userId)).usingElementComparator(BigDecimal::compareTo).containsExactly(new BigDecimal("200"));
    }

    private BigDecimal balance(final long userId) {
        return balances.findByUserId(userId).orElseThrow().balance();
    }

    private List<BigDecimal> remaining(final long userId) {
        return jdbc.queryForList("SELECT remaining_amount FROM point_recovery WHERE user_id = ? ORDER BY id", BigDecimal.class, userId);
    }

    private void await(final CountDownLatch latch) {
        try {
            if (!latch.await(10, TimeUnit.SECONDS)) {
                throw new IllegalStateException("동시 처리 대기 시간이 초과되었습니다.");
            }
        } catch (final InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("동시 처리 대기 중 인터럽트가 발생했습니다.", exception);
        }
    }

    private void earnAfterStart(final long userId, final long referenceId, final String amount, final CountDownLatch ready,
        final CountDownLatch start) throws InterruptedException {
        ready.countDown();
        if (!start.await(10, TimeUnit.SECONDS)) {
            throw new IllegalStateException("동시 적립 시작 대기 시간이 초과되었습니다.");
        }
        earn.execute(userId, PointType.REVIEW, referenceId, new BigDecimal(amount));
    }

    private void reclaimAfterStart(final long userId, final long reviewId, final CountDownLatch ready, final CountDownLatch start)
        throws InterruptedException {
        ready.countDown();
        if (!start.await(10, TimeUnit.SECONDS)) {
            throw new IllegalStateException("동시 회수 시작 대기 시간이 초과되었습니다.");
        }
        deduct.execute(userId, PointType.REVIEW, reviewId, new BigDecimal("500"));
    }
}
