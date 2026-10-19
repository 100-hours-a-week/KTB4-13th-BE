package com.book.core.user.infrastructure.persistence.repository;

import com.book.core.user.application.port.UserRepositoryPort;
import com.book.core.user.domain.User;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
class UserRepositoryAdapter implements UserRepositoryPort {
    private final UserJpaRepository jpaRepository;

    @Override
    public User save(final User user) {
        return jpaRepository.saveAndFlush(user);
    }

    @Override
    public Optional<User> findById(final Long id) {
        return jpaRepository.findById(id);
    }

    @Override
    public Optional<User> findByIdForUpdate(final Long id) {
        return jpaRepository.findByIdForUpdate(id);
    }
}
