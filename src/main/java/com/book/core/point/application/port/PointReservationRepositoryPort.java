package com.book.core.point.application.port;

import com.book.core.point.domain.PointReservation;
import java.util.Optional;

public interface PointReservationRepositoryPort {
    Optional<PointReservation> findByAttemptId(Long attemptId);

    void save(PointReservation reservation);
}
