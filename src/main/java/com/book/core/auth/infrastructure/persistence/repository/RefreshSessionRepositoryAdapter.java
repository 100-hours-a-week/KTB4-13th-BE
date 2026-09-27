package com.book.core.auth.infrastructure.persistence.repository;

import com.book.core.auth.application.port.RefreshSessionRepository;
import com.book.core.auth.domain.RefreshSession;
import com.book.core.auth.infrastructure.persistence.mapper.RefreshSessionPersistenceMapper;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
class RefreshSessionRepositoryAdapter implements RefreshSessionRepository {
    private final RefreshSessionJpaRepository repository;

    @Override
    public RefreshSession save(final RefreshSession refreshSession) {
        return RefreshSessionPersistenceMapper.toDomain(repository.saveAndFlush(RefreshSessionPersistenceMapper.toEntity(refreshSession)));
    }

    @Override
    public Optional<RefreshSession> findActiveByUserId(final Long userId) {
        return repository.findByUserIdAndRevokedAtIsNull(userId).map(RefreshSessionPersistenceMapper::toDomain);
    }

    @Override
    public Optional<RefreshSession> findActiveByTokenHash(final String tokenHash) {
        return repository.findByTokenHashAndRevokedAtIsNull(tokenHash).map(RefreshSessionPersistenceMapper::toDomain);
    }
}
