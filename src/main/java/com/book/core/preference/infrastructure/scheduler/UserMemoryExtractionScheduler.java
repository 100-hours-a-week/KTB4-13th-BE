package com.book.core.preference.infrastructure.scheduler;

import com.book.core.preference.application.service.UserPreferenceService;
import com.zaxxer.hikari.HikariDataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.memory-extraction.enabled", havingValue = "true")
@RequiredArgsConstructor
@Slf4j
public class UserMemoryExtractionScheduler {
    private static final String LOCK_NAME = "book_user_memory_extraction";

    private final HikariDataSource dataSource;
    private final UserPreferenceService userPreferenceService;

    @Scheduled(cron = "0 0 2 * * *", zone = "Asia/Seoul")
    public void runNightlyBatch() {
        try (Connection lockConnection = dataSource.getConnection()) {
            final boolean acquired;
            try {
                acquired = acquireLock(lockConnection);
            } catch (final SQLException exception) {
                dataSource.evictConnection(lockConnection);
                throw exception;
            }
            if (!acquired) {
                log.info("[USER_PREFERENCE] 다른 인스턴스가 야간 취향 배치를 실행 중이어서 건너뜁니다.");
                return;
            }
            try {
                userPreferenceService.runNightlyBatch();
            } finally {
                // 인터럽트 상태에서도 락 해제를 시도하고 정리 후 상태를 복원합니다.
                final boolean interrupted = Thread.interrupted();
                try {
                    if (!releaseLock(lockConnection)) {
                        dataSource.evictConnection(lockConnection);
                    }
                } finally {
                    if (interrupted) {
                        Thread.currentThread().interrupt();
                    }
                }
            }
        } catch (final Exception exception) {
            log.error("[USER_PREFERENCE] 야간 취향 배치 실행에 실패했습니다. 다음 실행에서 재시도합니다.", exception);
        }
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

    private boolean releaseLock(final Connection connection) {
        try (PreparedStatement statement = connection.prepareStatement("SELECT RELEASE_LOCK(?)")) {
            statement.setString(1, LOCK_NAME);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    log.warn("[USER_PREFERENCE] 야간 취향 배치 잠금 해제 결과가 없습니다.");
                    return false;
                }
                final int released = resultSet.getInt(1);
                if (resultSet.wasNull()) {
                    log.warn("[USER_PREFERENCE] 야간 취향 배치 잠금 해제가 NULL을 반환했습니다.");
                    return false;
                }
                if (released != 1) {
                    log.warn("[USER_PREFERENCE] 야간 취향 배치 잠금을 해제하지 못했습니다. result={}", released);
                    return false;
                }
                return true;
            }
        } catch (final SQLException exception) {
            log.warn("[USER_PREFERENCE] 야간 취향 배치 잠금 해제 중 오류가 발생했습니다.", exception);
            return false;
        }
    }
}
