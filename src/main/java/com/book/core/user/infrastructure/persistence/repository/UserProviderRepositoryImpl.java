package com.book.core.user.infrastructure.persistence.repository;

import com.book.common.exception.BusinessException;
import com.book.common.exception.CommonErrorCode;
import com.book.core.user.application.port.UserProviderRepository;
import com.book.core.user.domain.ProviderType;
import com.book.core.user.domain.UserProvider;
import com.book.core.user.domain.exception.UserErrorCode;
import com.book.core.user.infrastructure.persistence.mapper.UserProviderPersistenceMapper;
import jakarta.persistence.PersistenceException;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
class UserProviderRepositoryImpl implements UserProviderRepository {
    private static final String PROVIDER_IDENTITY_UNIQUE_CONSTRAINT = "uk_user_providers_identity_active_flag";

    private final UserProviderJpaRepository repository;

    @Override
    public UserProvider save(final UserProvider userProvider) {
        try {
            return UserProviderPersistenceMapper.toDomain(
                    repository.saveAndFlush(UserProviderPersistenceMapper.toEntity(userProvider)));
        } catch (final DataAccessException | PersistenceException exception) {
            if (ConstraintViolationDetector.hasConstraint(exception, PROVIDER_IDENTITY_UNIQUE_CONSTRAINT)) {
                throw new BusinessException(UserErrorCode.PROVIDER_IDENTITY_CONFLICT, exception);
            }
            throw new BusinessException(CommonErrorCode.STORAGE_FAILURE, exception);
        }
    }

    @Override
    public Optional<UserProvider> findActiveByProviderTypeAndProviderUserId(
            final ProviderType providerType, final String providerUserId) {
        try {
            return repository
                    .findByProviderTypeAndProviderUserIdAndDeletedAtIsNull(providerType, providerUserId)
                    .map(UserProviderPersistenceMapper::toDomain);
        } catch (final DataAccessException | PersistenceException exception) {
            throw new BusinessException(CommonErrorCode.STORAGE_FAILURE, exception);
        }
    }
}
