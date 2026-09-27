package com.book.core.product.infrastructure.scheduler;

import com.book.core.product.application.service.ProductService;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import javax.sql.DataSource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ProductPopularitySnapshotScheduler {
    private static final String LOCK_NAME = "book_product_popularity_snapshot_refresh";

    private final DataSource dataSource;
    private final ProductService productService;

    @EventListener(ApplicationReadyEvent.class)
    public void refreshOnStartup() {
        refreshSnapshot();
    }

    @Scheduled(cron = "0 0 * * * *", zone = "UTC")
    public void refreshHourly() {
        refreshSnapshot();
    }

    private void refreshSnapshot() {
        try (Connection lockConnection = dataSource.getConnection()) {
            if (!acquireLock(lockConnection)) {
                return;
            }
            try {
                productService.refreshProductPopularitySnapshot();
                log.info("상품 인기도 스냅샷을 갱신했습니다.");
            } finally {
                releaseLock(lockConnection);
            }
        } catch (final Exception exception) {
            log.error("상품 인기도 스냅샷 갱신에 실패했습니다.", exception);
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
                    log.warn("상품 인기도 스냅샷 갱신 잠금을 해제하지 못했습니다.");
                }
            }
        } catch (final SQLException exception) {
            log.warn("상품 인기도 스냅샷 갱신 잠금 해제 중 오류가 발생했습니다.", exception);
        }
    }
}
