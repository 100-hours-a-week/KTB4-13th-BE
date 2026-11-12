package com.book.core.book.infrastructure.scheduler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;

class MysqlNewBookImportLockTest {
    private static final String RELEASE_LOCK = "SELECT RELEASE_LOCK('book_new_book_collection')";
    private final JdbcTemplate jdbc = mock(JdbcTemplate.class);
    private final Connection connection = mock(Connection.class);
    private final Statement statement = mock(Statement.class);
    private final MysqlNewBookImportLock lock = new MysqlNewBookImportLock(jdbc);

    @BeforeEach
    void acquireLockAndFailToRelease() throws SQLException {
        final ResultSet acquired = mock(ResultSet.class);
        when(jdbc.execute(ArgumentMatchers.<ConnectionCallback<Boolean>>any()))
            .thenAnswer((final var invocation) -> invocation.getArgument(0, ConnectionCallback.class).doInConnection(connection));
        when(connection.createStatement()).thenReturn(statement);
        when(statement.executeQuery("SELECT GET_LOCK('book_new_book_collection', 0)")).thenReturn(acquired);
        when(acquired.next()).thenReturn(true);
        when(acquired.getObject(1)).thenReturn(1);
        when(acquired.getInt(1)).thenReturn(1);
        when(statement.executeQuery(RELEASE_LOCK)).thenThrow(new SQLException("잠금 해제 실패"));
    }

    @Test
    void 배치와_잠금_해제가_모두_실패하면_배치의_원래_예외를_유지한다() throws SQLException {
        final var batchFailure = new IllegalStateException("배치 실패");

        assertThatThrownBy(() -> lock.runIfAcquired(() -> {
            throw batchFailure;
        })).isSameAs(batchFailure);

        verify(statement).executeQuery(RELEASE_LOCK);
    }

    @Test
    void 배치가_완료된_경우_잠금_해제_예외를_별도로_처리한다() throws SQLException {
        final Runnable batch = mock(Runnable.class);

        assertThat(lock.runIfAcquired(batch)).isTrue();

        verify(batch).run();
        verify(statement).executeQuery(RELEASE_LOCK);
    }
}
