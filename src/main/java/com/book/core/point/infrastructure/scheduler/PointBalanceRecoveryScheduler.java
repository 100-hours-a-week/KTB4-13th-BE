package com.book.core.point.infrastructure.scheduler;

import com.book.core.point.application.service.PointService;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import javax.sql.DataSource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class PointBalanceRecoveryScheduler {
    private static final String LOCK_NAME = "book_point_balance_recovery";
    private static final Duration REGISTRATION_GRACE_PERIOD = Duration.ofMinutes(10);
    private static final int PAGE_SIZE = 500;

    private final DataSource dataSource;
    private final PointService pointService;
    private final Clock clock;

    @Scheduled(cron = "${app.signup-initialization-recovery.cron:0 */10 * * * *}")
    public void recover() {
        try (Connection lockConnection = dataSource.getConnection()) {
            if (!acquireLock(lockConnection)) {
                return;
            }
            try {
                pointService.recoverMissingPointBalances(registeredBefore(), PAGE_SIZE);
            } finally {
                releaseLock(lockConnection);
            }
        } catch (final Exception exception) {
            log.error("포인트 잔액 누락 복구 실행에 실패했습니다.", exception);
        }
    }

    // users.created_at은 Hibernate가 JVM 기본 시간대의 LocalDateTime으로 기록하므로 같은 시간대로 기준 시각을 계산합니다.
    private LocalDateTime registeredBefore() {
        return LocalDateTime.ofInstant(clock.instant(), ZoneId.systemDefault()).minus(REGISTRATION_GRACE_PERIOD);
    }

    private boolean acquireLock(final Connection connection) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("SELECT GET_LOCK(?, 0)")) {
            statement.setString(1, LOCK_NAME);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    throw new SQLException("GET_LOCK 결과가 없습니다");
                }
                final int acquired = resultSet.getInt(1);
                if (resultSet.wasNull()) {
                    throw new SQLException("GET_LOCK이 NULL을 반환했습니다");
                }
                return acquired == 1;
            }
        }
    }

    private void releaseLock(final Connection connection) {
        try (PreparedStatement statement = connection.prepareStatement("SELECT RELEASE_LOCK(?)")) {
            statement.setString(1, LOCK_NAME);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next() || resultSet.getInt(1) != 1) {
                    log.warn("포인트 잔액 누락 복구 잠금을 해제하지 못했습니다.");
                }
            }
        } catch (final SQLException exception) {
            log.warn("포인트 잔액 누락 복구 잠금 해제 중 오류가 발생했습니다.", exception);
        }
    }
}
