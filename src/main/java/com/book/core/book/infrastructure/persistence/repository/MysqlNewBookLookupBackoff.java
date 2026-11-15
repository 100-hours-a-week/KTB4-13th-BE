package com.book.core.book.infrastructure.persistence.repository;

import com.book.core.book.application.port.NewBookLookupBackoff;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
class MysqlNewBookLookupBackoff implements NewBookLookupBackoff {
    private static final int MISSING_DETAIL_WAIT_DAYS = 7;
    private static final int MAX_FAILURE_WAIT_DAYS = 4;
    private final JdbcTemplate jdbc;

    @Override
    public boolean isWaiting(final String isbn13, final LocalDate today) {
        final Integer waiting = jdbc.queryForObject("""
            SELECT EXISTS(SELECT 1 FROM new_book_lookup_backoff WHERE isbn13 = ? AND retry_on > ?)
            """, Integer.class, isbn13, today);
        return waiting != null && waiting == 1;
    }

    @Override
    public void deferMissingDetail(final String isbn13, final LocalDate today) {
        final LocalDate retryOn = today.plusDays(MISSING_DETAIL_WAIT_DAYS);
        jdbc.update("""
            INSERT INTO new_book_lookup_backoff (isbn13, failure_count, retry_on) VALUES (?, 0, ?) AS next
            ON DUPLICATE KEY UPDATE failure_count = 0, retry_on = next.retry_on
            """, isbn13, retryOn);
    }

    @Override
    public void deferAfterFailure(final String isbn13, final LocalDate today) {
        // 실패할 때마다 1일, 2일, 4일로 대기 기간을 늘리고 4일에서 멈춥니다.
        jdbc.update("""
            INSERT INTO new_book_lookup_backoff (isbn13, failure_count, retry_on) VALUES (?, 1, ? + INTERVAL 1 DAY)
            ON DUPLICATE KEY UPDATE retry_on = ? + INTERVAL LEAST(1 << failure_count, ?) DAY, failure_count = failure_count + 1
            """, isbn13, today, today, MAX_FAILURE_WAIT_DAYS);
    }
}
