package com.book.core.user.infrastructure.persistence.repository;

import com.book.common.exception.BusinessException;
import com.book.core.user.application.port.UserRepositoryPort;
import com.book.core.user.domain.User;
import com.book.core.user.domain.exception.UserErrorCode;
import jakarta.persistence.PersistenceException;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
class UserRepositoryAdapter implements UserRepositoryPort {
    private static final String NICKNAME_UNIQUE_CONSTRAINT = "uk_users_nickname_active_flag";

    private final UserJpaRepository jpaRepository;

    @Override
    public User save(final User user) {
        try {
            return jpaRepository.saveAndFlush(user);
        } catch (final DataAccessException | PersistenceException exception) {
            if (ConstraintViolationDetector.hasConstraint(exception, NICKNAME_UNIQUE_CONSTRAINT)) {
                throw new BusinessException(UserErrorCode.NICKNAME_CONFLICT, exception);
            }
            throw exception;
        }
    }

    @Override
    public Optional<User> findById(final Long id) {
        return jpaRepository.findById(id);
    }
}
