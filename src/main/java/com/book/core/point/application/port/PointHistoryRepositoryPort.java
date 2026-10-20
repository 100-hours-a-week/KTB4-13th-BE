package com.book.core.point.application.port;

import com.book.core.point.domain.PointHistory;
import com.book.core.point.domain.enums.PointType;
import java.util.Objects;
import java.util.List;
import java.util.Optional;

public interface PointHistoryRepositoryPort {
    List<PointHistory> findByUserId(final Long userId);

    default List<PointHistory> findByUserIdAndTypeAndReferenceId(final Long userId, final PointType type, final Long referenceId) {
        return findByUserId(userId).stream()
            .filter((final PointHistory history) -> history.type() == type && Objects.equals(history.referenceId(), referenceId)).toList();
    }

    default Optional<PointHistory> findByUserIdAndEventKey(final Long userId, final String eventKey) {
        return Optional.empty();
    }

    PointHistory save(final PointHistory history);
}
