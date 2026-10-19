package com.book.core.memory.infrastructure.persistence.repository;

import com.book.core.memory.application.port.UserMemoryRepositoryPort;
import com.book.core.memory.domain.UserMemory;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
class UserMemoryRepositoryAdapter implements UserMemoryRepositoryPort {
    private final UserMemoryJpaRepository jpaRepository;

    @Override
    public void lockByUserId(final Long userId) {
        jpaRepository.lockUserById(userId);
    }

    @Override
    public List<UserMemory> findActiveByUserId(final Long userId) {
        return jpaRepository.findAllByUserIdAndDeletedAtIsNull(userId);
    }

    @Override
    public List<UserMemory> findDeletedByUserIdAndConversationId(final Long userId, final String conversationId) {
        return jpaRepository.findAllByUserIdAndConversationIdAndDeletedAtIsNotNull(userId, conversationId);
    }

    @Override
    public void deleteActiveByUserIdAndConversationId(final Long userId, final String conversationId) {
        jpaRepository.deleteActiveByUserIdAndConversationId(userId, conversationId);
    }

    @Override
    public List<UserMemory> saveAll(final List<UserMemory> memories) {
        return jpaRepository.saveAll(memories);
    }
}
