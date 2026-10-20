package com.book.core.agentchat.infrastructure.persistence.repository;

import com.book.core.agentchat.domain.ChatSession;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

public interface ChatSessionJpaRepository extends JpaRepository<ChatSession, Long> {
    @Query("select s from ChatSession s where s.userId = :userId and s.deletedAt is null "
        + "and (:beforeId is null or s.id < :beforeId) order by s.id desc")
    List<ChatSession> findSessions(Long userId, Long beforeId, Pageable page);

    @Lock(LockModeType.OPTIMISTIC_FORCE_INCREMENT)
    @Query("select s from ChatSession s where s.id = :sessionId")
    Optional<ChatSession> findForChangeById(Long sessionId);
}
