package com.book.core.preference.infrastructure.scheduler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.book.core.preference.application.service.UserPreferenceService;
import com.zaxxer.hikari.HikariDataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class UserMemoryExtractionSchedulerTest {
    private final HikariDataSource pool = mock(HikariDataSource.class);
    private final UserPreferenceService service = mock(UserPreferenceService.class);
    private final Connection connection = mock(Connection.class);
    private final PreparedStatement acquire = mock(PreparedStatement.class);
    private final PreparedStatement release = mock(PreparedStatement.class);
    private final ResultSet acquired = mock(ResultSet.class);
    private final ResultSet released = mock(ResultSet.class);
    private final UserMemoryExtractionScheduler scheduler = new UserMemoryExtractionScheduler(pool, service);

    private void setup() throws SQLException {
        when(pool.getConnection()).thenReturn(connection);
        when(connection.prepareStatement("SELECT GET_LOCK(?, 0)")).thenReturn(acquire);
        when(acquire.executeQuery()).thenReturn(acquired);
        when(acquired.next()).thenReturn(true);
        when(acquired.getInt(1)).thenReturn(1);
        when(connection.prepareStatement("SELECT RELEASE_LOCK(?)")).thenReturn(release);
        when(release.executeQuery()).thenReturn(released);
        when(released.next()).thenReturn(true);
        when(released.getInt(1)).thenReturn(1);
    }

    @AfterEach
    void clearInterrupt() {
        Thread.interrupted();
    }

    @Test
    void 예외에도_같은_연결에서_해제한다() throws SQLException {
        setup();
        doThrow(new IllegalStateException()).when(service).runNightlyBatch();
        scheduler.runNightlyBatch();
        var order = inOrder(acquire, service, release, connection);
        order.verify(acquire).executeQuery();
        order.verify(service).runNightlyBatch();
        order.verify(release).executeQuery();
        order.verify(connection).close();
        verify(pool, never()).evictConnection(any());
    }

    @Test
    void 정상_종료시_해제하고_연결을_반환한다() throws SQLException {
        setup();
        scheduler.runNightlyBatch();
        verify(release).executeQuery();
        verify(connection).close();
        verify(pool, never()).evictConnection(any());
    }

    @Test
    void 해제_결과가_0이면_연결을_폐기한다() throws SQLException {
        setup();
        when(released.getInt(1)).thenReturn(0);
        scheduler.runNightlyBatch();
        verify(pool).evictConnection(connection);
        verify(connection).close();
    }

    @Test
    void 해제_결과가_null이면_연결을_폐기한다() throws SQLException {
        setup();
        when(released.getInt(1)).thenReturn(0);
        when(released.wasNull()).thenReturn(true);
        scheduler.runNightlyBatch();
        verify(pool).evictConnection(connection);
        verify(connection).close();
    }

    @Test
    void 해제_중_SQL_예외가_발생하면_연결을_폐기한다() throws SQLException {
        setup();
        when(release.executeQuery()).thenThrow(new SQLException("failure"));
        scheduler.runNightlyBatch();
        verify(pool).evictConnection(connection);
        verify(connection).close();
    }

    @Test
    void 인터럽트를_보류하고_정리한_뒤_복원한다() throws SQLException {
        setup();
        doAnswer(invocation -> {
            Thread.currentThread().interrupt();
            return null;
        }).when(service).runNightlyBatch();
        when(release.executeQuery()).thenAnswer(invocation -> {
            assertThat(Thread.currentThread().isInterrupted()).isFalse();
            return released;
        });
        scheduler.runNightlyBatch();
        assertThat(Thread.currentThread().isInterrupted()).isTrue();
        verify(release).executeQuery();
        verify(connection).close();
    }

    @Test
    void 획득_결과가_불확실하면_연결을_폐기한다() throws SQLException {
        setup();
        when(acquire.executeQuery()).thenThrow(new SQLException("unknown"));
        scheduler.runNightlyBatch();
        verify(pool).evictConnection(connection);
        verifyNoInteractions(service);
    }
}
