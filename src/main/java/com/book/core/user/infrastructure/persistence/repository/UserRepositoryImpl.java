package com.book.core.user.infrastructure.persistence.repository;

import com.book.common.exception.BusinessException;
import com.book.common.exception.CommonErrorCode;
import com.book.core.user.application.port.UserRepository;
import com.book.core.user.domain.User;
import com.book.core.user.infrastructure.persistence.mapper.UserPersistenceMapper;
import jakarta.persistence.PersistenceException;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
class UserRepositoryImpl implements UserRepository {
    private final UserJpaRepository repository;

    @Override
    public User save(final User user) {
        try {
            return UserPersistenceMapper.toDomain(repository.saveAndFlush(UserPersistenceMapper.toEntity(user)));
        } catch (final DataAccessException | PersistenceException exception) {
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
