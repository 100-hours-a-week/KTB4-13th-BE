package com.book.core.product.infrastructure.scheduler;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.book.core.product.application.service.ProductService;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;

class ProductPopularitySnapshotSchedulerTest {
    private static final String LOCK_NAME = "book_product_popularity_snapshot_refresh";

    private final DataSource dataSource = mock(DataSource.class);
    private final ProductService productService = mock(ProductService.class);
    private final ProductPopularitySnapshotScheduler scheduler = new ProductPopularitySnapshotScheduler(dataSource, productService);

    @Test
    void DB_잠금을_얻으면_인기도_스냅샷을_갱신하고_잠금을_해제한다() throws SQLException {
        final Connection connection = mock(Connection.class);
        final PreparedStatement acquireStatement = mock(PreparedStatement.class);
        final ResultSet acquireResult = mock(ResultSet.class);
        final PreparedStatement releaseStatement = mock(PreparedStatement.class);
        final ResultSet releaseResult = mock(ResultSet.class);
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.prepareStatement("SELECT GET_LOCK(?, 0)")).thenReturn(acquireStatement);
        when(acquireStatement.executeQuery()).thenReturn(acquireResult);
        when(acquireResult.next()).thenReturn(true);
        when(acquireResult.getInt(1)).thenReturn(1);
        when(connection.prepareStatement("SELECT RELEASE_LOCK(?)")).thenReturn(releaseStatement);
        when(releaseStatement.executeQuery()).thenReturn(releaseResult);
        when(releaseResult.next()).thenReturn(true);
        when(releaseResult.getInt(1)).thenReturn(1);

        scheduler.refreshHourly();

        verify(productService).refreshProductPopularitySnapshot();
        verify(acquireStatement).setString(1, LOCK_NAME);
        verify(releaseStatement).setString(1, LOCK_NAME);
        verify(connection).close();
    }

    @Test
    void 다른_인스턴스가_잠금을_가지고_있으면_갱신을_건너뛴다() throws SQLException {
        final Connection connection = mock(Connection.class);
        final PreparedStatement acquireStatement = mock(PreparedStatement.class);
        final ResultSet acquireResult = mock(ResultSet.class);
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.prepareStatement("SELECT GET_LOCK(?, 0)")).thenReturn(acquireStatement);
        when(acquireStatement.executeQuery()).thenReturn(acquireResult);
        when(acquireResult.next()).thenReturn(true);
        when(acquireResult.getInt(1)).thenReturn(0);

        scheduler.refreshHourly();

        verify(productService, never()).refreshProductPopularitySnapshot();
        verify(connection, never()).prepareStatement("SELECT RELEASE_LOCK(?)");
        verify(connection).close();
    }
}
