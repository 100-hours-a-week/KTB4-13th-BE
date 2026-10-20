package com.book.core.point.infrastructure.persistence.repository;

import com.book.core.point.domain.PointHistory;
import com.book.core.point.domain.enums.PointType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PointHistoryJpaRepository extends JpaRepository<PointHistory, Long> {
    List<PointHistory> findByUserId(final Long userId);

    List<PointHistory> findByUserIdAndTypeAndReferenceId(final Long userId, final PointType type, final Long referenceId);

    Optional<PointHistory> findByUserIdAndEventKeyAndDeletedAtIsNull(final Long userId, final String eventKey);
}
