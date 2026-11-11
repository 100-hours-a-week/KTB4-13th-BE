package com.book.core.preference.infrastructure.scheduler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

import com.book.core.preference.application.service.UserPreferenceService;
import com.zaxxer.hikari.HikariDataSource;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;

@Testcontainers
@Tag("integration")
class UserMemoryExtractionSchedulerIntegrationTest {
    private static final String LOCK = "book_user_memory_extraction";
    @Container
    static final MySQLContainer mysql = new MySQLContainer("mysql:8.4.8");

    static class CapturingPool extends HikariDataSource {
        Connection borrowed;

        @Override
        public Connection getConnection() throws SQLException {
            borrowed = super.getConnection();
            return borrowed;
        }
    }

    @Test
    void 실제_해제_NULL_응답시_풀에서_연결을_폐기하고_새_물리_연결을_사용한다() throws Exception {
        try (CapturingPool pool = pool()) {
            UserPreferenceService service = mock(UserPreferenceService.class);
            long[] oldId = new long[1];
            doAnswer(invocation -> {
                oldId[0] = scalar(pool.borrowed, "SELECT CONNECTION_ID()");
                assertThat(scalar(pool.borrowed, "SELECT RELEASE_LOCK('" + LOCK + "')")).isEqualTo(1);
                return null;
            }).when(service).runNightlyBatch();
            new UserMemoryExtractionScheduler(pool, service).runNightlyBatch();
            try (Connection next = pool.getConnection()) {
                assertThat(scalar(next, "SELECT CONNECTION_ID()")).isNotEqualTo(oldId[0]);
                assertThat(scalar(next, "SELECT GET_LOCK('" + LOCK + "', 5)")).isEqualTo(1);
                assertThat(scalar(next, "SELECT RELEASE_LOCK('" + LOCK + "')")).isEqualTo(1);
            }
        }
    }

    @Test
    void 실제_배치_중에는_다른_연결이_락을_얻지_못하고_종료_후에는_얻는다() throws Exception {
        try (CapturingPool pool = pool();
            Connection other = DriverManager.getConnection(mysql.getJdbcUrl(), mysql.getUsername(), mysql.getPassword())) {
            UserPreferenceService service = mock(UserPreferenceService.class);
            long[] id = new long[1];
            doAnswer(invocation -> {
                id[0] = scalar(pool.borrowed, "SELECT CONNECTION_ID()");
                assertThat(scalar(other, "SELECT GET_LOCK('" + LOCK + "', 0)")).isZero();
                return null;
            }).when(service).runNightlyBatch();
            new UserMemoryExtractionScheduler(pool, service).runNightlyBatch();
            try (Connection next = pool.getConnection()) {
                assertThat(scalar(next, "SELECT CONNECTION_ID()")).isEqualTo(id[0]);
            }
            assertThat(scalar(other, "SELECT GET_LOCK('" + LOCK + "', 0)")).isEqualTo(1);
            assertThat(scalar(other, "SELECT RELEASE_LOCK('" + LOCK + "')")).isEqualTo(1);
        }
    }

    private CapturingPool pool() {
        CapturingPool pool = new CapturingPool();
        pool.setJdbcUrl(mysql.getJdbcUrl());
        pool.setUsername(mysql.getUsername());
        pool.setPassword(mysql.getPassword());
        pool.setMaximumPoolSize(1);
        pool.setMinimumIdle(0);
        return pool;
    }

    private long scalar(Connection connection, String sql) throws SQLException {
        try (Statement statement = connection.createStatement(); ResultSet result = statement.executeQuery(sql)) {
            assertThat(result.next()).isTrue();
            return result.getLong(1);
        }
    }
}
