package com.book.core.point.infrastructure.persistence.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.book.common.exception.CoreException;
import com.book.core.point.application.usecase.ConfirmPaymentPointsUseCase;
import com.book.core.point.application.usecase.DeductPointUseCase;
import com.book.core.point.application.usecase.EarnPointUseCase;
import com.book.core.point.application.usecase.ReclaimPaymentBonusUseCase;
import com.book.core.point.application.usecase.ReservePaymentPointsUseCase;
import com.book.core.point.application.usecase.RestorePaymentPointsUseCase;
import com.book.core.point.domain.PointBalance;
import com.book.core.point.domain.enums.PointType;
import java.math.BigDecimal;
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
@ActiveProfiles({"test", "mysql-test"})
@Testcontainers
@Tag("integration")
@Tag("mysql-migration")
class PointWholeAmountMySqlIntegrationTest {
    @Container
    @ServiceConnection
    static final MySQLContainer mysql = new MySQLContainer("mysql:8.4.8");
    @Autowired
    private PointBalanceJpaRepository balances;
    @Autowired
    private ReservePaymentPointsUseCase reserve;
    @Autowired
    private ConfirmPaymentPointsUseCase confirm;
    @Autowired
    private EarnPointUseCase earn;
    @Autowired
    private DeductPointUseCase deduct;
    @Autowired
    private RestorePaymentPointsUseCase restore;
    @Autowired
    private ReclaimPaymentBonusUseCase reclaim;
    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void 기존_정수_범위를_넘는_결제_보너스도_정확히_적립한다() {
        final long userId = 790002L;
        balances.save(new PointBalance(null, userId, new BigDecimal("9999999000")));

        earn.execute(userId, PointType.PAYMENT, 700L, new BigDecimal("2000"));

        assertThat(jdbc.queryForObject("SELECT balance FROM point_balance WHERE user_id = ?", BigDecimal.class, userId))
            .isEqualByComparingTo("10000001000");
        assertThat(jdbc.queryForObject("SELECT amount FROM point_history WHERE user_id = ?", BigDecimal.class, userId))
            .isEqualByComparingTo("2000");
        assertThat(jdbc.queryForObject("SELECT balance_after FROM point_history WHERE user_id = ?", BigDecimal.class, userId))
            .isEqualByComparingTo("10000001000");
    }

    @Test
    void 소수_포인트는_모든_진입점에서_거절되고_잔액과_이력을_변경하지_않는다() {
        final long userId = 790001L;
        balances.save(new PointBalance(null, userId, new BigDecimal("1000")));
        final BigDecimal fraction = new BigDecimal("0.5");

        assertThatThrownBy(() -> reserve.execute(userId, 1L, fraction, 1L)).isInstanceOf(CoreException.class);
        assertThatThrownBy(() -> earn.execute(userId, PointType.REVIEW, 2L, fraction)).isInstanceOf(CoreException.class);
        assertThatThrownBy(() -> deduct.execute(userId, PointType.REVIEW, 3L, fraction)).isInstanceOf(CoreException.class);
        assertThatThrownBy(() -> restore.execute(userId, 4L, fraction)).isInstanceOf(CoreException.class);
        assertThatThrownBy(() -> reclaim.execute(userId, 5L, fraction)).isInstanceOf(CoreException.class);

        assertThat(jdbc.queryForObject("SELECT balance FROM point_balance WHERE user_id = ?", BigDecimal.class, userId))
            .isEqualByComparingTo("1000");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM point_history WHERE user_id = ?", Long.class, userId)).isZero();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM point_reservation WHERE user_id = ?", Long.class, userId)).isZero();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM point_recovery WHERE user_id = ?", Long.class, userId)).isZero();

        reserve.execute(userId, 6L, new BigDecimal("1.00"), 7L);
        confirm.execute(userId, 6L);
        assertThat(jdbc.queryForObject("SELECT balance FROM point_balance WHERE user_id = ?", BigDecimal.class, userId))
            .isEqualByComparingTo("999");
        assertThat(jdbc.queryForObject("SELECT amount FROM point_history WHERE user_id = ?", BigDecimal.class, userId))
            .isEqualByComparingTo("-1");
    }
}
