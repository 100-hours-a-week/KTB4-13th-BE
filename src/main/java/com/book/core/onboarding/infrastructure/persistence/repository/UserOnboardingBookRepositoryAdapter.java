package com.book.core.onboarding.infrastructure.persistence.repository;

import com.book.core.onboarding.application.port.UserOnboardingBookRepositoryPort;
import com.book.core.onboarding.domain.UserOnboardingBook;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
class UserOnboardingBookRepositoryAdapter implements UserOnboardingBookRepositoryPort {
    private final UserOnboardingBookJpaRepository jpaRepository;

    @Override
    public List<UserOnboardingBook> findByUserId(final Long userId) {
        return jpaRepository.findByUserId(userId);
    }

    @Override
    public void deleteByUserId(final Long userId) {
        jpaRepository.deleteByUserId(userId);
    }

    @Override
    public List<UserOnboardingBook> saveAll(final List<UserOnboardingBook> books) {
        return jpaRepository.saveAll(books);
    }
}
