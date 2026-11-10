package com.book.core.point.application.port;

import com.book.core.point.domain.PointEarnEvent;
import com.book.core.point.domain.enums.PointType;
import java.util.Optional;

public interface PointEarnEventRepositoryPort {
    Optional<PointEarnEvent> find(Long userId, PointType type, Long referenceId);

    void save(PointEarnEvent event);
}
