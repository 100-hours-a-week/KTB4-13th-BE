package com.book.core.point.infrastructure.persistence.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.book.core.point.application.usecase.DeductPointUseCase;
import com.book.core.point.application.usecase.EarnPointUseCase;
import com.book.core.point.domain.PointBalance;
import com.book.core.point.domain.enums.PointType;
import java.math.BigDecimal;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

@SpringBootTest
@ActiveProfiles("test")
@Tag("integration")
class PointBalanceOptimisticLockIntegrationTest {
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

    private void apply(final boolean deduct, final Long userId, final Long referenceId, final BigDecimal amount) {
        if (deduct) {
            deductPointUseCase.execute(userId, PointType.REVIEW, referenceId, amount);
            return;
        }
        earnPointUseCase.execute(userId, PointType.REVIEW, referenceId, amount);
    }
}
