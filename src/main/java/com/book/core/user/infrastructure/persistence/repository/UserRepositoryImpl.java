package com.book.core.user.infrastructure.persistence.repository;

import com.book.common.exception.BusinessException;
import com.book.common.exception.CommonErrorCode;
import com.book.core.user.application.port.UserRepository;
import com.book.core.user.domain.User;
import com.book.core.user.domain.exception.UserErrorCode;
import com.book.core.user.infrastructure.persistence.mapper.UserPersistenceMapper;
import jakarta.persistence.PersistenceException;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
class UserRepositoryImpl implements UserRepository {
    private static final String NICKNAME_UNIQUE_CONSTRAINT = "uk_users_nickname_active_flag";

    private final UserJpaRepository repository;

    @Override
    public User save(final User user) {
        try {
            return UserPersistenceMapper.toDomain(repository.saveAndFlush(UserPersistenceMapper.toEntity(user)));
        } catch (final DataAccessException | PersistenceException exception) {
            if (ConstraintViolationDetector.hasConstraint(exception, NICKNAME_UNIQUE_CONSTRAINT)) {
                throw new BusinessException(UserErrorCode.NICKNAME_CONFLICT, exception);
            }
            throw new BusinessException(CommonErrorCode.STORAGE_FAILURE, exception);
        }
    }

    @Override
    public Optional<User> findById(final Long id) {
        try {
            return repository.findById(id).map(UserPersistenceMapper::toDomain);
        } catch (final DataAccessException | PersistenceException exception) {
            throw new BusinessException(CommonErrorCode.STORAGE_FAILURE, exception);
        }
    }
}
