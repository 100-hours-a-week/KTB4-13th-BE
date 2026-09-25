package com.book.core.auth.infrastructure.persistence.repository;

import com.book.common.exception.BusinessException;
import com.book.common.exception.CommonErrorCode;
import com.book.core.auth.application.port.RefreshSessionRepository;
import com.book.core.auth.domain.RefreshSession;
import com.book.core.auth.infrastructure.persistence.mapper.RefreshSessionPersistenceMapper;
import jakarta.persistence.PersistenceException;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
class RefreshSessionRepositoryImpl implements RefreshSessionRepository {
    private final RefreshSessionJpaRepository repository;

    @Override
    public RefreshSession save(final RefreshSession refreshSession) {
        try {
            return RefreshSessionPersistenceMapper.toDomain(
                    repository.saveAndFlush(RefreshSessionPersistenceMapper.toEntity(refreshSession)));
        } catch (final DataAccessException | PersistenceException exception) {
            throw new BusinessException(CommonErrorCode.STORAGE_FAILURE, exception);
        }
    }

    @Override
    public Optional<RefreshSession> findActiveByUserId(final Long userId) {
        try {
            return repository.findByUserIdAndRevokedAtIsNull(userId).map(RefreshSessionPersistenceMapper::toDomain);
        } catch (final DataAccessException | PersistenceException exception) {
            throw new BusinessException(CommonErrorCode.STORAGE_FAILURE, exception);
        }
    }

    @Override
    public Optional<RefreshSession> findActiveByTokenHash(final String tokenHash) {
        try {
            return repository
                    .findByTokenHashAndRevokedAtIsNull(tokenHash)
                    .map(RefreshSessionPersistenceMapper::toDomain);
        } catch (final DataAccessException | PersistenceException exception) {
            throw new BusinessException(CommonErrorCode.STORAGE_FAILURE, exception);
        }
    }

    @Override
    public Optional<RefreshSession> findActiveByTokenHashForUpdate(final String tokenHash) {
        try {
            return repository.findActiveByTokenHashForUpdate(tokenHash).map(RefreshSessionPersistenceMapper::toDomain);
        } catch (final DataAccessException | PersistenceException exception) {
            throw new BusinessException(CommonErrorCode.STORAGE_FAILURE, exception);
        }
    }
}
