package com.book.core.point.infrastructure.persistence.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import com.book.core.point.application.usecase.ConfirmPaymentPointsUseCase;
import com.book.core.point.application.usecase.EarnPointUseCase;
import com.book.core.point.application.usecase.ReservePaymentPointsUseCase;
import com.book.core.point.domain.PointBalance;
import com.book.core.point.domain.enums.PointType;
import java.math.BigDecimal;
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
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;

@SpringBootTest
@ActiveProfiles({"test", "mysql-test"})
@Testcontainers
@Tag("integration")
@Tag("mysql-migration")
class PointReservationMySqlIntegrationTest {
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
    private JdbcTemplate jdbc;

    @Test
    void 동일_잔액을_동시에_예약하면_하나만_성공하고_잔액과_예약_기록이_일치한다() throws Exception {
        final long userId = 8_100_001L;
        balances.save(new PointBalance(null, userId, new BigDecimal("1000")));
        final CountDownLatch ready = new CountDownLatch(2);
        final CountDownLatch start = new CountDownLatch(1);
        try (final var executor = Executors.newFixedThreadPool(2)) {
            final var first = executor.submit(() -> attemptReserve(userId, 8_101_001L, ready, start));
            final var second = executor.submit(() -> attemptReserve(userId, 8_101_002L, ready, start));
            assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
            start.countDown();

            assertThat(java.util.List.of(first.get(20, TimeUnit.SECONDS), second.get(20, TimeUnit.SECONDS))).containsExactlyInAnyOrder(true,
                false);
        }
        final PointBalance balance = balances.findByUserId(userId).orElseThrow();
        assertThat(balance.balance()).isEqualByComparingTo("1000");
        assertThat(balance.reserved()).isEqualByComparingTo("1000");
        assertThat(balance.available()).isEqualByComparingTo("0");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM point_reservation WHERE user_id = ?", Long.class, userId)).isEqualTo(1L);
    }

    @Test
    void 음수_적립_조정이_예약을_침범하면_롤백하고_기존_예약은_확정할_수_있다() {
        final long userId = 8_100_002L;
        balances.save(new PointBalance(null, userId, new BigDecimal("1000")));
        reserve.execute(userId, 8_102_001L, new BigDecimal("800"), 8_102_002L);

        assertThatThrownBy(() -> earn.execute(userId, PointType.REVIEW, 8_102_003L, new BigDecimal("-300"))).isInstanceOfSatisfying(
            CoreException.class,
            (final CoreException exception) -> assertThat(exception.errorCode()).isEqualTo(ErrorCode.POINT_EXCEEDS_BALANCE));
        final PointBalance beforeConfirm = balances.findByUserId(userId).orElseThrow();
        assertThat(beforeConfirm.balance()).isEqualByComparingTo("1000");
        assertThat(beforeConfirm.reserved()).isEqualByComparingTo("800");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM point_history WHERE user_id = ?", Long.class, userId)).isZero();

        confirm.execute(userId, 8_102_001L);
        final PointBalance afterConfirm = balances.findByUserId(userId).orElseThrow();
        assertThat(afterConfirm.balance()).isEqualByComparingTo("200");
        assertThat(afterConfirm.reserved()).isEqualByComparingTo("0");
    }

    private boolean attemptReserve(final long userId, final long attemptId, final CountDownLatch ready, final CountDownLatch start)
        throws InterruptedException {
        ready.countDown();
        if (!start.await(10, TimeUnit.SECONDS)) {
            throw new IllegalStateException("동시 예약 시작 대기 시간이 초과되었습니다.");
        }
        try {
            reserve.execute(userId, attemptId, new BigDecimal("1000"), attemptId);
            return true;
        } catch (final CoreException exception) {
            assertThat(exception.errorCode()).isEqualTo(ErrorCode.POINT_EXCEEDS_BALANCE);
            return false;
        }
    }
}
