package com.book.core.point.infrastructure.persistence.repository;

import com.book.core.point.domain.PointReservation;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PointReservationJpaRepository extends JpaRepository<PointReservation, Long> {
    Optional<PointReservation> findByAttemptId(Long attemptId);
}
