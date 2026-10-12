package com.book.core.point.application.port;

import com.book.core.point.domain.PointHistory;

import java.util.List;

public interface PointHistoryRepositoryPort {
    List<PointHistory> findByUserId(final Long userId);

    void save(final PointHistory history);
}
