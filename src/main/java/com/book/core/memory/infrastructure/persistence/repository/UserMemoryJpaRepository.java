package com.book.core.memory.infrastructure.persistence.repository;

import com.book.core.memory.domain.UserMemory;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface UserMemoryJpaRepository extends JpaRepository<UserMemory, Long> {
    List<UserMemory> findAllByUserIdAndConversationIdAndDeletedAtIsNotNull(final Long userId, final String conversationId);

    @Modifying
    @Query("""
        DELETE FROM UserMemory m
        WHERE m.userId = :userId AND m.conversationId = :conversationId AND m.deletedAt IS NULL
        """)
    void deleteActiveByUserIdAndConversationId(@Param("userId") final Long userId, @Param("conversationId") final String conversationId);
}
