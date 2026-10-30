package com.book.core.point.infrastructure.persistence.repository;

import com.book.core.point.domain.PointEarnEvent;
import com.book.core.point.domain.enums.PointType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PointEarnEventJpaRepository extends JpaRepository<PointEarnEvent, Long> {
    Optional<PointEarnEvent> findByUserIdAndTypeAndReferenceId(Long userId, PointType type, Long referenceId);
}
