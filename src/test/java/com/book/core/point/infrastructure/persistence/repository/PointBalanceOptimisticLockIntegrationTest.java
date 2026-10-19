package com.book.core.point.infrastructure.persistence.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.book.common.exception.CoreException;
import com.book.core.point.application.usecase.DeductPointUseCase;
import com.book.core.point.application.usecase.EarnPointUseCase;
import com.book.core.point.domain.PointBalance;
import com.book.core.point.domain.enums.PointType;
import java.math.BigDecimal;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
@Tag("integration")
class PointBalanceOptimisticLockIntegrationTest {
    @Container
    @ServiceConnection
    static final MySQLContainer mysql = new MySQLContainer("mysql:8.4.8");

    @Autowired
    private PointBalanceJpaRepository balances;
    @Autowired
    private EarnPointUseCase earnPointUseCase;
    @Autowired
    private DeductPointUseCase deductPointUseCase;
    @Autowired
    private JdbcTemplate jdbc;
    @Autowired
    private PlatformTransactionManager transactionManager;

    @ParameterizedTest
    @CsvSource({"false, 940001, 1250", "true, 940002, 750"})
    void 먼저_읽은_잔액의_갱신이_충돌하면_그_트랜잭션의_이력도_롤백한다(final boolean deduct, final Long userId, final BigDecimal expectedBalance) {
        balances.save(new PointBalance(null, userId, new BigDecimal("1000")));
        final TransactionTemplate transaction = new TransactionTemplate(transactionManager);
        transaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

        assertThatThrownBy(() -> transaction.executeWithoutResult((final TransactionStatus outer) -> {
            assertThat(balances.findByUserId(userId).orElseThrow().version()).isZero();
            transaction.executeWithoutResult((final TransactionStatus inner) -> apply(deduct, userId, 1L, new BigDecimal("250")));
            apply(deduct, userId, 2L, new BigDecimal("100"));
        })).isInstanceOf(OptimisticLockingFailureException.class);

        final PointBalance balance = balances.findByUserId(userId).orElseThrow();
        assertThat(balance.balance()).isEqualByComparingTo(expectedBalance);
        assertThat(balance.version()).isEqualTo(1L);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM point_history WHERE user_id = ? AND reference_id = 1", Long.class, userId))
            .isEqualTo(1L);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM point_history WHERE user_id = ? AND reference_id = 2", Long.class, userId))
            .isZero();
        assertThat(jdbc.queryForObject("SELECT balance_after FROM point_history WHERE user_id = ?", BigDecimal.class, userId))
            .isEqualByComparingTo(expectedBalance);
    }

    @ParameterizedTest
    @CsvSource({"0, 940003", "0.0, 940004", "0.00, 940005"})
    void 자릿수가_다른_0원은_잔액_버전과_이력을_변경하지_않는다(final BigDecimal amount, final Long userId) {
        balances.save(new PointBalance(null, userId, new BigDecimal("1000")));

        earnPointUseCase.execute(userId, PointType.REVIEW, 1L, amount);
        deductPointUseCase.execute(userId, PointType.REVIEW, 2L, amount);

        final PointBalance balance = balances.findByUserId(userId).orElseThrow();
        assertThat(balance.balance()).isEqualByComparingTo("1000");
        assertThat(balance.version()).isZero();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM point_history WHERE user_id = ?", Long.class, userId)).isZero();
    }

    @Test
    void 최초_적립이_동시에_들어와도_잔액_행_하나에_두_적립과_이력을_모두_반영한다() throws Exception {
        final Long userId = 940006L;
        final CountDownLatch earned = new CountDownLatch(1);
        final CountDownLatch commit = new CountDownLatch(1);
        final CompletableFuture<Void> first = CompletableFuture
            .runAsync(() -> new TransactionTemplate(transactionManager).executeWithoutResult((final TransactionStatus status) -> {
                earnPointUseCase.execute(userId, PointType.REVIEW, 1L, new BigDecimal("100"));
                earned.countDown();
                await(commit);
            }));
        await(earned);

        final CompletableFuture<Void> second = CompletableFuture
            .runAsync(() -> new TransactionTemplate(transactionManager).executeWithoutResult((final TransactionStatus status) -> {
                // 결제·취소처럼 적립 전에 다른 조회로 읽기 시점이 먼저 정해진 트랜잭션을 재현합니다.
                jdbc.queryForObject("SELECT COUNT(*) FROM point_history WHERE user_id = ?", Long.class, userId);
                earnPointUseCase.execute(userId, PointType.PAYMENT, 2L, new BigDecimal("200"));
            }));
        Thread.sleep(500);
        final boolean secondFinishedBeforeCommit = second.isDone();
        commit.countDown();
        first.get(10, TimeUnit.SECONDS);
        second.get(10, TimeUnit.SECONDS);

        assertThat(secondFinishedBeforeCommit).isFalse();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM point_balance WHERE user_id = ?", Long.class, userId)).isEqualTo(1L);
        final PointBalance balance = balances.findByUserId(userId).orElseThrow();
        assertThat(balance.balance()).isEqualByComparingTo("300");
        assertThat(balance.version()).isEqualTo(2L);
        assertThat(jdbc.queryForList("SELECT balance_after FROM point_history WHERE user_id = ? ORDER BY id", BigDecimal.class, userId))
            .usingElementComparator(BigDecimal::compareTo).containsExactly(new BigDecimal("100"), new BigDecimal("300"));
    }

    @Test
    void 잔액_행이_없으면_차감하지_않고_잔액도_만들지_않는다() {
        final Long userId = 940007L;

        assertThatThrownBy(() -> deductPointUseCase.execute(userId, PointType.PAYMENT, 1L, BigDecimal.TEN))
            .isInstanceOf(CoreException.class);

        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM point_balance WHERE user_id = ?", Long.class, userId)).isZero();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM point_history WHERE user_id = ?", Long.class, userId)).isZero();
    }

    private static void await(final CountDownLatch latch) {
        try {
            if (!latch.await(10, TimeUnit.SECONDS)) {
                throw new IllegalStateException("대기 시간을 초과했습니다");
            }
        } catch (final InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(exception);
        }
    }

    private void apply(final boolean deduct, final Long userId, final Long referenceId, final BigDecimal amount) {
        if (deduct) {
            deductPointUseCase.execute(userId, PointType.REVIEW, referenceId, amount);
            return;
        }
        earnPointUseCase.execute(userId, PointType.REVIEW, referenceId, amount);
    }
}
