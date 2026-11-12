package com.book.core.book.infrastructure.persistence.repository;

import com.book.core.book.application.port.BookDetailCallBudget;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
class BookDetailCallBudgetAdapter implements BookDetailCallBudget {
    private static final String QUOTA_DAY = "DATE(UTC_TIMESTAMP() + INTERVAL 9 HOUR)";
    private final JdbcTemplate jdbc;
    private final int dailyLimit;

    BookDetailCallBudgetAdapter(final JdbcTemplate jdbc, @Value("${app.new-book.detail-daily-limit:500}") final int dailyLimit) {
        if (dailyLimit < 1 || dailyLimit > 30000) {
            throw new IllegalArgumentException("Book detail daily limit must be between 1 and 30000");
        }
        this.jdbc = jdbc;
        this.dailyLimit = dailyLimit;
    }

    @Override
    public boolean tryAcquire() {
        return jdbc.update("""
            UPDATE book_detail_call_budget
            SET calls = IF(quota_day = %1$s, calls + 1, 1), quota_day = %1$s
            WHERE id = 1 AND (next_call_at IS NULL OR next_call_at <= UTC_TIMESTAMP(6))
                AND (quota_day IS NULL OR quota_day <> %1$s OR calls < ?)
            """.formatted(QUOTA_DAY), dailyLimit) == 1;
    }

    @Override
    public void pauseAfterRateLimit() {
        jdbc.update("""
            UPDATE book_detail_call_budget SET next_call_at = UTC_TIMESTAMP(6) + INTERVAL 15 MINUTE WHERE id = 1
            """);
    }
}
