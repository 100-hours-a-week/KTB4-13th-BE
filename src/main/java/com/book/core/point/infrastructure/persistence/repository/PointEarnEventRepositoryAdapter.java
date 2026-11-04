package com.book.core.point.infrastructure.persistence.repository;

import com.book.core.point.application.port.PointEarnEventRepositoryPort;
import com.book.core.point.domain.PointEarnEvent;
import com.book.core.point.domain.enums.PointType;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class PointEarnEventRepositoryAdapter implements PointEarnEventRepositoryPort {
    private final PointEarnEventJpaRepository repository;

    @Override
    public Optional<PointEarnEvent> find(final Long userId, final PointType type, final Long referenceId) {
        return repository.findByUserIdAndTypeAndReferenceId(userId, type, referenceId);
    }

    @Override
    public void save(final PointEarnEvent event) {
        repository.save(event);
    }
}
