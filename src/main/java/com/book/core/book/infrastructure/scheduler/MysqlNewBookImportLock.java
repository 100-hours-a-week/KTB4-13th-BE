package com.book.core.book.infrastructure.scheduler;

import com.book.core.book.application.port.NewBookImportLock;
import java.sql.SQLException;
import java.sql.Statement;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
class MysqlNewBookImportLock implements NewBookImportLock {
    private final JdbcTemplate jdbc;

    @Override
    public boolean runIfAcquired(final Runnable batch) {
        return Boolean.TRUE.equals(jdbc.execute((ConnectionCallback<Boolean>) connection -> {
            try (Statement statement = connection.createStatement()) {
                try (var result = statement.executeQuery("SELECT GET_LOCK('book_new_book_collection', 0)")) {
                    if (!result.next() || result.getObject(1) == null) {
                        throw new SQLException("신간 도서 등록 배치 잠금 획득 결과를 확인할 수 없습니다.");
                    }
                    if (result.getInt(1) != 1) {
                        return false;
                    }
                }
                try {
                    batch.run();
                    return true;
                } finally {
                    releaseLock(statement);
                }
            }
        }));
    }

    private void releaseLock(final Statement statement) {
        try (var result = statement.executeQuery("SELECT RELEASE_LOCK('book_new_book_collection')")) {
            if (!result.next() || result.getInt(1) != 1) {
                log.warn("[BOOK] 신간 도서 등록 배치 잠금을 해제하지 못했습니다.");
            }
        } catch (final SQLException exception) {
            log.warn("[BOOK] 신간 도서 등록 배치 잠금 해제 중 오류가 발생했습니다.", exception);
        }
    }
}
