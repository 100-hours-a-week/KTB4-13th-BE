package com.book.core.user.infrastructure.scheduler;

import com.book.core.user.application.service.UserService;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import javax.sql.DataSource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class UserDeletionScheduler {
    private static final String LOCK_NAME = "book_user_withdrawal_completion";

    private final DataSource dataSource;
    private final UserService userService;

    @Scheduled(cron = "${app.user-withdrawal.cron:0 0 4 * * *}", zone = "Asia/Seoul")
    public void completeUserDeletion() {
        try (Connection lockConnection = dataSource.getConnection()) {
            if (!acquireLock(lockConnection)) {
                log.info("[USER_DELETION] 다른 인스턴스가 회원 최종 탈퇴를 실행 중이어서 건너뜁니다.");
                return;
            }
            try {
                userService.completeUserDeletion();
            } finally {
                releaseLock(lockConnection);
            }
        } catch (final Exception exception) {
            log.error("[USER_DELETION] 회원 최종 탈퇴 스케줄 실행에 실패했습니다. 다음 실행에서 재시도합니다.", exception);
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

    private void releaseLock(final Connection connection) {
        try (PreparedStatement statement = connection.prepareStatement("SELECT RELEASE_LOCK(?)")) {
            statement.setString(1, LOCK_NAME);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next() || resultSet.getInt(1) != 1) {
                    log.warn("[USER_DELETION] 회원 최종 탈퇴 잠금을 해제하지 못했습니다.");
                }
            }
        } catch (final SQLException exception) {
            log.warn("[USER_DELETION] 회원 최종 탈퇴 잠금 해제 중 오류가 발생했습니다.", exception);
        }
    }
}
