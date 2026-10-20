package com.book.core.agentchat.infrastructure.persistence.repository;

import com.book.core.agentchat.application.result.MemoryExtractionTarget;
import com.book.core.agentchat.domain.ChatSession;
import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface ChatSessionJpaRepository extends JpaRepository<ChatSession, Long> {
    @Query("select s from ChatSession s where s.userId = :userId and s.deletedAt is null "
        + "and (:beforeId is null or s.id < :beforeId) order by s.id desc")
    List<ChatSession> findSessions(Long userId, Long beforeId, Pageable page);

    @Lock(LockModeType.OPTIMISTIC_FORCE_INCREMENT)
    @Query("select s from ChatSession s where s.id = :sessionId")
    Optional<ChatSession> findForChangeById(Long sessionId);

    // 진행 중인 대화의 낙관적 잠금과 충돌하지 않도록 version을 올리지 않습니다.
    @Modifying
    @Query("update ChatSession s set s.memoryExtractedSequence = :sequence "
        + "where s.id = :sessionId and s.userId = :userId and s.memoryExtractedSequence < :sequence")
    int markMemoryExtracted(Long userId, Long sessionId, long sequence);

    @Query("select new com.book.core.agentchat.application.result.MemoryExtractionTarget(s.userId, s.id, max(m.sequence)) "
        + "from ChatSession s join ChatMessage m on m.sessionId = s.id "
        + "where s.deletedAt is null and m.createdAt < :before and m.sequence > s.memoryExtractedSequence "
        + "group by s.id, s.userId order by max(m.createdAt), s.id")
    List<MemoryExtractionTarget> findMemoryExtractionTargets(LocalDateTime before);
}
