package com.book.core.user.infrastructure.persistence.repository;

import com.book.common.exception.BusinessException;
import com.book.core.user.application.port.UserProviderRepositoryPort;
import com.book.core.user.domain.ProviderType;
import com.book.core.user.domain.UserProvider;
import com.book.core.user.domain.exception.UserErrorCode;
import jakarta.persistence.PersistenceException;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
class UserProviderRepositoryAdapter implements UserProviderRepositoryPort {
    private static final String PROVIDER_IDENTITY_UNIQUE_CONSTRAINT = "uk_user_providers_identity_active_flag";

    private final UserProviderJpaRepository jpaRepository;

    @Override
    public UserProvider save(final UserProvider userProvider) {
        try {
            return jpaRepository.saveAndFlush(userProvider);
        } catch (final DataAccessException | PersistenceException exception) {
            if (ConstraintViolationDetector.hasConstraint(exception, PROVIDER_IDENTITY_UNIQUE_CONSTRAINT)) {
                throw new BusinessException(UserErrorCode.PROVIDER_IDENTITY_CONFLICT, exception);
            }
            throw exception;
        }
    }

    @Override
    public Optional<UserProvider> findActiveByProviderTypeAndProviderUserId(
            final ProviderType providerType, final String providerUserId) {
        return jpaRepository.findByProviderTypeAndProviderUserIdAndDeletedAtIsNull(providerType, providerUserId);
    }
}
