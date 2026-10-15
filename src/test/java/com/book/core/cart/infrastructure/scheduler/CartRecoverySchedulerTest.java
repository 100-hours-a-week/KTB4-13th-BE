package com.book.core.cart.infrastructure.scheduler;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.book.core.cart.application.service.CartService;
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
import org.springframework.dao.DataAccessResourceFailureException;

class CartRecoverySchedulerTest {
    private static final Instant NOW = Instant.parse("2030-01-01T00:30:00Z");

    private final DataSource dataSource = mock(DataSource.class);
    private final CartService cartService = mock(CartService.class);
    private final Connection connection = mock(Connection.class);
    private final PreparedStatement acquireStatement = mock(PreparedStatement.class);
    private final ResultSet acquireResult = mock(ResultSet.class);
    private final PreparedStatement releaseStatement = mock(PreparedStatement.class);
    private final ResultSet releaseResult = mock(ResultSet.class);
    private final CartRecoveryScheduler scheduler =
        new CartRecoveryScheduler(dataSource, cartService, Clock.fixed(NOW, ZoneId.of("Pacific/Kiritimati")));

    @Test
    void 서비스_Clock의_시간대와_관계없이_JVM_기본_시간대로_가입_10분_기준_시각을_계산한다() throws SQLException {
        givenLock(1);
        givenRelease(1);

        scheduler.recover();

        verify(cartService).recoverMissingCarts(LocalDateTime.ofInstant(NOW, ZoneId.systemDefault()).minusMinutes(10), 500);
        verify(releaseStatement).executeQuery();
        verify(connection).close();
    }

    @Test
    void GET_LOCK이_NULL을_반환하면_예외를_전파하지_않고_복구를_건너뛴다() throws SQLException {
        givenLock(0);
        when(acquireResult.wasNull()).thenReturn(true);

        assertThatCode(scheduler::recover).doesNotThrowAnyException();

        verify(cartService, never()).recoverMissingCarts(any(), anyInt());
        verify(connection).close();
    }

    @Test
    void 복구_중_예외가_발생해도_잠금을_해제하고_예외를_전파하지_않는다() throws SQLException {
        givenLock(1);
        givenRelease(1);
        doThrow(new DataAccessResourceFailureException("users")).when(cartService).recoverMissingCarts(any(), anyInt());

        assertThatCode(scheduler::recover).doesNotThrowAnyException();

        verify(releaseStatement).executeQuery();
        verify(connection).close();
    }

    @Test
    void 잠금_해제에_실패해도_예외를_전파하지_않는다() throws SQLException {
        givenLock(1);
        when(connection.prepareStatement("SELECT RELEASE_LOCK(?)")).thenReturn(releaseStatement);
        when(releaseStatement.executeQuery()).thenThrow(new SQLException("release"));

        assertThatCode(scheduler::recover).doesNotThrowAnyException();

        verify(cartService).recoverMissingCarts(any(), anyInt());
        verify(connection).close();
    }

    @Test
    void DB_연결을_얻지_못하면_예외를_전파하지_않고_복구를_건너뛴다() throws SQLException {
        when(dataSource.getConnection()).thenThrow(new SQLException("connection"));

        assertThatCode(scheduler::recover).doesNotThrowAnyException();

        verify(cartService, never()).recoverMissingCarts(any(), anyInt());
    }

    private void givenLock(final int acquired) throws SQLException {
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.prepareStatement("SELECT GET_LOCK(?, 0)")).thenReturn(acquireStatement);
        when(acquireStatement.executeQuery()).thenReturn(acquireResult);
        when(acquireResult.next()).thenReturn(true);
        when(acquireResult.getInt(1)).thenReturn(acquired);
    }

    private void givenRelease(final int released) throws SQLException {
        when(connection.prepareStatement("SELECT RELEASE_LOCK(?)")).thenReturn(releaseStatement);
        when(releaseStatement.executeQuery()).thenReturn(releaseResult);
        when(releaseResult.next()).thenReturn(true);
        when(releaseResult.getInt(1)).thenReturn(released);
    }
}
