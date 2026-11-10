package com.book.core.point.infrastructure.persistence.repository;

import com.book.core.point.application.port.PointReservationRepositoryPort;
import com.book.core.point.domain.PointReservation;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class PointReservationRepositoryAdapter implements PointReservationRepositoryPort {
    private final PointReservationJpaRepository repository;

    @Override
    public Optional<PointReservation> findByAttemptId(final Long attemptId) {
        return repository.findByAttemptId(attemptId);
    }

    @Override
    public void save(final PointReservation reservation) {
        repository.save(reservation);
    }
}
