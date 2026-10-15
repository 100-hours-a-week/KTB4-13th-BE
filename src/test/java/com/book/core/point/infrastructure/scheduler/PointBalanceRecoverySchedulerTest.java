package com.book.core.point.infrastructure.scheduler;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.book.core.point.application.service.PointService;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;

class PointBalanceRecoverySchedulerTest {
    private static final Instant NOW = Instant.parse("2030-01-01T00:30:00Z");

    private final DataSource dataSource = mock(DataSource.class);
    private final PointService pointService = mock(PointService.class);
    private final Connection connection = mock(Connection.class);
    private final PreparedStatement acquireStatement = mock(PreparedStatement.class);
    private final ResultSet acquireResult = mock(ResultSet.class);
    private final PointBalanceRecoveryScheduler scheduler =
        new PointBalanceRecoveryScheduler(dataSource, pointService, Clock.fixed(NOW, ZoneId.of("Pacific/Kiritimati")));

    @Test
    void 서비스_Clock의_시간대와_관계없이_JVM_기본_시간대로_가입_10분_기준_시각을_계산한다() throws SQLException {
        givenLock(1);
        final PreparedStatement releaseStatement = mock(PreparedStatement.class);
        final ResultSet releaseResult = mock(ResultSet.class);
        when(connection.prepareStatement("SELECT RELEASE_LOCK(?)")).thenReturn(releaseStatement);
        when(releaseStatement.executeQuery()).thenReturn(releaseResult);
        when(releaseResult.next()).thenReturn(true);
        when(releaseResult.getInt(1)).thenReturn(1);

        scheduler.recover();

        verify(pointService).recoverMissingPointBalances(LocalDateTime.ofInstant(NOW, ZoneId.systemDefault()).minusMinutes(10), 500);
        verify(releaseStatement).executeQuery();
        verify(connection).close();
    }

    private void givenLock(final int acquired) throws SQLException {
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.prepareStatement("SELECT GET_LOCK(?, 0)")).thenReturn(acquireStatement);
        when(acquireStatement.executeQuery()).thenReturn(acquireResult);
        when(acquireResult.next()).thenReturn(true);
        when(acquireResult.getInt(1)).thenReturn(acquired);
    }
}
