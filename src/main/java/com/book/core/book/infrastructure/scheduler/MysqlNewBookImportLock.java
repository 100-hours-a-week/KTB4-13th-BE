package com.book.core.book.infrastructure.scheduler;

import com.book.core.book.application.port.NewBookImportLock;
import java.sql.SQLException;
import java.sql.Statement;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

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
                        throw new SQLException("Cannot acquire new book collection lock");
                    }
                    if (result.getInt(1) != 1) {
                        return false;
                    }
                }
                try {
                    batch.run();
                    return true;
                } finally {
                    statement.executeQuery("SELECT RELEASE_LOCK('book_new_book_collection')").close();
                }
            }
        }));
    }
}
