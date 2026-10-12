package com.book.core.point.infrastructure.persistence.repository;

import com.book.core.point.domain.PointHistory;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PointHistoryJpaRepository extends JpaRepository<PointHistory, Long> {
    List<PointHistory> findByUserId(final Long userId);
}
